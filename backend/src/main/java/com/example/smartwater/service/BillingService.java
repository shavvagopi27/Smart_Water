package com.example.smartwater.service;

import com.example.smartwater.dto.BillingDtos.*;
import com.example.smartwater.entity.*;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.*;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

/**
 * Billing engine. For one billing cycle and each active household it calculates:
 * metered consumption -> tiered tariff charge -> shared purchase cost share -> adjustment -> final amount,
 * and stores the result as an itemised invoice.
 */
@Service
@RequiredArgsConstructor
public class BillingService {
    private final BillingCycleRepository cycleRepository;
    private final ApartmentService apartmentService;
    private final HouseholdRepository householdRepository;
    private final WaterUsageLogRepository usageRepository;
    private final TariffPlanRepository tariffRepository;
    private final WaterPurchaseRepository purchaseRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final InvoicePdfService pdfService;
    private final EmailService emailService;

    @Value("${app.billing.payment-instructions}")
    private String paymentInstructions;

    private record Usage(boolean hasReadings, BigDecimal consumption, BigDecimal previous, BigDecimal current) {}

    // ------------------------------------------------------------------ cycles

    @Transactional
    public CycleResponse createCycle(CycleRequest req) {
        Apartment apartment = apartmentService.get(req.apartmentId());
        if (req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("End date must not be before the start date");
        }
        if (cycleRepository.existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                apartment.getId(), req.endDate(), req.startDate())) {
            throw new ConflictException("Another billing cycle already covers part of this period");
        }
        BillingCycle c = new BillingCycle();
        c.setApartment(apartment);
        c.setName(req.name().trim());
        c.setStartDate(req.startDate());
        c.setEndDate(req.endDate());
        c.setStatus(CycleStatus.OPEN);
        return cycleResponse(cycleRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<CycleResponse> listCycles(Long apartmentId) {
        List<BillingCycle> list = apartmentId == null ? cycleRepository.findAllByOrderByStartDateDesc()
                : cycleRepository.findByApartmentIdOrderByStartDateDesc(apartmentId);
        return list.stream().map(this::cycleResponse).toList();
    }

    @Transactional(readOnly = true)
    public CycleResponse getCycle(Long id) {
        return cycleResponse(getCycleEntity(id));
    }

    @Transactional
    public CycleResponse finalizeCycle(Long id) {
        BillingCycle cycle = getCycleEntity(id);
        requireOpen(cycle);
        if (invoiceRepository.countByBillingCycleId(id) == 0) {
            generateInvoices(id, null);
        }
        cycle.setStatus(CycleStatus.FINALIZED);
        cycle.setFinalizedAt(Instant.now());
        cycleRepository.save(cycle);
        for (Invoice inv : invoiceRepository.findByBillingCycleIdOrderByHouseholdIdAsc(id)) {
            emailService.sendBillGenerated(inv, pdfService.render(inv));
        }
        return cycleResponse(cycle);
    }

    @Transactional
    public CycleResponse archiveCycle(Long id) {
        BillingCycle cycle = getCycleEntity(id);
        if (cycle.getStatus() != CycleStatus.FINALIZED) {
            throw new ConflictException("Only a FINALIZED billing cycle can be archived (current status: " + cycle.getStatus() + ")");
        }
        cycle.setStatus(CycleStatus.ARCHIVED);
        return cycleResponse(cycleRepository.save(cycle));
    }

    // ------------------------------------------------------------------ invoice generation

    /** (Re)generates all invoices of an OPEN cycle. Existing draft invoices of the cycle are replaced. */
    @Transactional
    public List<InvoiceResponse> generateInvoices(Long cycleId, GenerateRequest request) {
        BillingCycle cycle = getCycleEntity(cycleId);
        requireOpen(cycle);
        Long apartmentId = cycle.getApartment().getId();

        TariffPlan plan = tariffRepository.findByApartmentIdAndActiveTrue(apartmentId).stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No active tariff plan is configured for this apartment"));
        List<TariffCalculator.TierDef> tiers = plan.getTiers().stream()
                .map(t -> new TariffCalculator.TierDef(t.getTierOrder(), t.getFromKl(), t.getToKl(), t.getRatePerKl())).toList();

        List<Household> households = householdRepository.findByApartmentIdAndActiveTrueOrderByFlatNumberAsc(apartmentId);
        if (households.isEmpty()) {
            throw new BadRequestException("The apartment has no active households to bill");
        }

        Map<Long, AdjustmentRequest> adjustments = new HashMap<>();
        if (request != null && request.adjustments() != null) {
            Set<Long> valid = new HashSet<>();
            households.forEach(h -> valid.add(h.getId()));
            for (AdjustmentRequest a : request.adjustments()) {
                if (!valid.contains(a.householdId())) {
                    throw new BadRequestException("Adjustment refers to unknown household " + a.householdId());
                }
                adjustments.put(a.householdId(), a);
            }
        }

        // 1) usage per household
        Map<Long, Usage> usage = new HashMap<>();
        for (Household h : households) {
            List<WaterUsageLog> logs = usageRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(
                    h.getId(), cycle.getStartDate(), cycle.getEndDate());
            if (logs.isEmpty()) {
                BigDecimal ref = h.getMeter() != null ? h.getMeter().getCurrentReading() : BigDecimal.ZERO;
                usage.put(h.getId(), new Usage(false, BigDecimal.ZERO.setScale(3), ref, ref));
            } else {
                WaterUsageLog first = logs.get(0);
                WaterUsageLog last = logs.get(logs.size() - 1);
                BigDecimal total = logs.stream().map(WaterUsageLog::getConsumption).reduce(BigDecimal.ZERO, BigDecimal::add);
                usage.put(h.getId(), new Usage(true, total, first.getMeterReading().subtract(first.getConsumption()), last.getMeterReading()));
            }
        }

        // 2) shared purchase cost distribution
        BigDecimal totalPurchaseCost = purchaseRepository.findForCycle(apartmentId, cycleId, cycle.getStartDate(), cycle.getEndDate())
                .stream().map(WaterPurchase::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CostDistributor.Input> inputs = households.stream()
                .map(h -> new CostDistributor.Input(h.getId(), usage.get(h.getId()).consumption(), usage.get(h.getId()).hasReadings(), h.getAreaSqft()))
                .toList();
        Map<Long, CostDistributor.Share> shares = CostDistributor.distribute(totalPurchaseCost, inputs);
        BigDecimal measuredTotal = usage.values().stream().filter(Usage::hasReadings).map(Usage::consumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3) replace previous drafts
        invoiceRepository.deleteAll(invoiceRepository.findByBillingCycleIdOrderByHouseholdIdAsc(cycleId));
        invoiceRepository.flush();

        // 4) build invoices
        String period = YearMonth.from(cycle.getEndDate()).toString().replace("-", "");
        List<Invoice> created = new ArrayList<>();
        for (Household h : households) {
            Usage u = usage.get(h.getId());
            TariffCalculator.Result calc = TariffCalculator.calculate(tiers, u.consumption());
            CostDistributor.Share share = shares.get(h.getId());
            AdjustmentRequest adj = adjustments.get(h.getId());
            BigDecimal adjustment = adj == null ? BigDecimal.ZERO.setScale(2) : adj.amount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = calc.total().add(share.amount()).add(adjustment);
            if (total.signum() < 0) {
                throw new BadRequestException("Adjustment makes the invoice of flat " + h.getFlatNumber() + " negative");
            }

            Invoice inv = new Invoice();
            inv.setHousehold(h);
            inv.setBillingCycle(cycle);
            inv.setInvoiceNumber(String.format("INV-%s-%d-%04d", period, cycle.getId(), h.getId()));
            inv.setResidentName(residentNames(h.getId()));
            inv.setPreviousReading(u.previous());
            inv.setCurrentReading(u.current());
            inv.setConsumption(u.consumption());
            inv.setWaterCharge(calc.total());
            inv.setSharedCost(share.amount());
            inv.setAdjustment(adjustment);
            inv.setAdjustmentReason(adj == null ? null : adj.reason());
            inv.setTotalAmount(total);
            inv.setSharedCostBasis(totalPurchaseCost.signum() <= 0 ? SharedCostBasis.NONE
                    : share.areaFallback() ? SharedCostBasis.AREA_FALLBACK : SharedCostBasis.METERED);

            int order = 1;
            for (TariffCalculator.TierCharge c : calc.charges()) {
                String range = c.to() == null ? "above " + plain(c.from()) : plain(c.from()) + " to " + plain(c.to());
                addItem(inv, InvoiceItemType.TARIFF_TIER, "Water charge - tier " + c.order() + " (" + range + " kL)",
                        c.units(), c.rate(), c.amount(), order++);
            }
            if (inv.getSharedCostBasis() != SharedCostBasis.NONE) {
                String desc;
                if (inv.getSharedCostBasis() == SharedCostBasis.METERED) {
                    BigDecimal pct = measuredTotal.signum() == 0 ? BigDecimal.ZERO
                            : u.consumption().multiply(BigDecimal.valueOf(100)).divide(measuredTotal, 2, RoundingMode.HALF_UP);
                    desc = "Shared water purchase cost (" + pct.toPlainString() + "% of measured consumption)";
                } else {
                    desc = "Shared water purchase cost (flat-area fallback share)";
                }
                addItem(inv, InvoiceItemType.SHARED_COST, desc, null, null, share.amount(), order++);
            }
            if (adjustment.signum() != 0) {
                addItem(inv, InvoiceItemType.ADJUSTMENT, "Adjustment: " + adj.reason(), null, null, adjustment, order);
            }
            created.add(invoiceRepository.save(inv));
        }
        return created.stream().map(i -> Mappers.toInvoice(i, paymentInstructions)).toList();
    }

    // ------------------------------------------------------------------ helpers

    public CycleResponse cycleResponse(BillingCycle c) {
        BigDecimal purchases = purchaseRepository.findForCycle(c.getApartment().getId(), c.getId(), c.getStartDate(), c.getEndDate())
                .stream().map(WaterPurchase::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CycleResponse(c.getId(), c.getApartment().getId(), c.getName(), c.getStartDate(), c.getEndDate(),
                c.getStatus(), c.getFinalizedAt(), invoiceRepository.countByBillingCycleId(c.getId()),
                purchases, invoiceRepository.sumTotalsByCycle(c.getId()));
    }

    public BillingCycle getCycleEntity(Long id) {
        return cycleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Billing cycle not found"));
    }

    private void requireOpen(BillingCycle cycle) {
        if (cycle.getStatus() != CycleStatus.OPEN) {
            throw new ConflictException("Billing cycle '" + cycle.getName() + "' is " + cycle.getStatus() + " and can no longer be modified");
        }
    }

    private String residentNames(Long householdId) {
        List<String> names = userRepository.findByHouseholdIdOrderByIdAsc(householdId).stream().map(User::getName).toList();
        return names.isEmpty() ? "Unassigned" : String.join(", ", names);
    }

    private void addItem(Invoice inv, InvoiceItemType type, String description, BigDecimal qty, BigDecimal rate,
                         BigDecimal amount, int order) {
        InvoiceItem it = new InvoiceItem();
        it.setInvoice(inv);
        it.setItemType(type);
        it.setDescription(description);
        it.setQuantity(qty);
        it.setRate(rate);
        it.setAmount(amount);
        it.setSortOrder(order);
        inv.getItems().add(it);
    }

    private String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
