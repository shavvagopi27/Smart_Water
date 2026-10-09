package com.example.smartwater.service;

import com.example.smartwater.dto.AlertDtos.AlertResponse;
import com.example.smartwater.dto.BillingDtos.CycleResponse;
import com.example.smartwater.dto.BillingDtos.InvoiceResponse;
import com.example.smartwater.dto.DashboardDtos.*;
import com.example.smartwater.dto.TipDtos.TipResponse;
import com.example.smartwater.dto.UsageDtos.DailyPoint;
import com.example.smartwater.entity.*;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.repository.*;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final HouseholdRepository householdRepository;
    private final WaterUsageLogRepository usageRepository;
    private final TariffPlanRepository tariffRepository;
    private final InvoiceRepository invoiceRepository;
    private final AlertRepository alertRepository;
    private final WaterSavingTipRepository tipRepository;
    private final WaterPurchaseRepository purchaseRepository;
    private final BillingCycleRepository cycleRepository;
    private final UserRepository userRepository;
    private final BillingService billingService;

    @Value("${app.billing.payment-instructions}")
    private String paymentInstructions;

    @Transactional(readOnly = true)
    public ResidentDashboard resident() {
        AuthUser me = SecurityUtils.currentUser();
        if (me.householdId() == null) {
            throw new BadRequestException("Your account is not assigned to a household yet. Please ask your apartment admin to add you.");
        }
        Household h = householdRepository.findById(me.householdId()).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        String userName = userRepository.findById(me.id()).map(User::getName).orElse("Resident");

        Optional<WaterUsageLog> latest = usageRepository.findTopByHouseholdIdOrderByReadingDateDesc(h.getId());
        BigDecimal mtd = sum(usageRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(h.getId(), monthStart, today));

        BigDecimal estimate = tariffRepository.findByApartmentIdAndActiveTrue(h.getApartment().getId()).stream().findFirst()
                .map(p -> TariffCalculator.calculate(p.getTiers().stream()
                        .map(t -> new TariffCalculator.TierDef(t.getTierOrder(), t.getFromKl(), t.getToKl(), t.getRatePerKl())).toList(), mtd).total())
                .orElse(null);

        // apartment comparison: logs of all active households over the last 6 months
        List<Long> ids = householdRepository.findByApartmentIdAndActiveTrueOrderByFlatNumberAsc(h.getApartment().getId())
                .stream().map(Household::getId).toList();
        LocalDate sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1);
        List<WaterUsageLog> aptLogs = usageRepository.findByHouseholdIdInAndReadingDateBetweenOrderByReadingDateAsc(ids, sixMonthsAgo, today);

        List<ComparisonPoint> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today.minusMonths(i));
            List<WaterUsageLog> inMonth = aptLogs.stream().filter(l -> YearMonth.from(l.getReadingDate()).equals(ym)).toList();
            BigDecimal mine = sum(inMonth.stream().filter(l -> l.getHousehold().getId().equals(h.getId())).toList());
            monthly.add(new ComparisonPoint(ym.toString(), mine, averagePerHousehold(inMonth)));
        }
        BigDecimal aptAvgMtd = averagePerHousehold(aptLogs.stream().filter(l -> !l.getReadingDate().isBefore(monthStart)).toList());

        List<DailyPoint> daily = usageRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(h.getId(), today.minusDays(29), today)
                .stream().map(l -> new DailyPoint(l.getReadingDate(), l.getConsumption())).toList();

        InvoiceResponse latestInvoice = invoiceRepository.findByHouseholdIdOrderByIdDesc(h.getId()).stream().findFirst()
                .map(i -> Mappers.toInvoice(i, paymentInstructions)).orElse(null);
        List<AlertResponse> alerts = alertRepository.findByHouseholdIdOrderByIdDesc(h.getId()).stream().limit(5).map(Mappers::toAlert).toList();
        List<TipResponse> tips = tipRepository.findAllByOrderByIdAsc().stream().limit(3).map(Mappers::toTip).toList();

        return new ResidentDashboard(userName, h.getId(), h.getFlatNumber(),
                latest.map(WaterUsageLog::getReadingDate).orElse(null),
                latest.map(WaterUsageLog::getConsumption).orElse(null),
                mtd, estimate, aptAvgMtd, latestInvoice,
                alertRepository.countByHouseholdIdAndReadFlagFalse(h.getId()), alerts, daily, monthly, tips);
    }

    @Transactional(readOnly = true)
    public AdminDashboard admin() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<Household> households = householdRepository.findByActiveTrue();
        List<Long> ids = households.stream().map(Household::getId).toList();
        List<WaterUsageLog> logs = ids.isEmpty() ? List.of()
                : usageRepository.findByHouseholdIdInAndReadingDateBetweenOrderByReadingDateAsc(ids, monthStart, today);

        Map<Long, BigDecimal> perHousehold = new HashMap<>();
        for (WaterUsageLog l : logs) perHousehold.merge(l.getHousehold().getId(), l.getConsumption(), BigDecimal::add);

        BigDecimal purchased = purchaseRepository.findAllByOrderByPurchaseDateDesc().stream()
                .filter(p -> !p.getPurchaseDate().isBefore(monthStart) && !p.getPurchaseDate().isAfter(monthStart.plusMonths(1).minusDays(1)))
                .map(WaterPurchase::getVolumeKl).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<HouseholdUsage> usages = households.stream()
                .map(h -> new HouseholdUsage(h.getId(), h.getFlatNumber(), perHousehold.getOrDefault(h.getId(), BigDecimal.ZERO)))
                .sorted(Comparator.comparing(HouseholdUsage::consumption).reversed()).toList();
        BigDecimal avg = usages.isEmpty() ? BigDecimal.ZERO
                : sum(logs).divide(BigDecimal.valueOf(usages.size()), 2, RoundingMode.HALF_UP);
        List<ComparisonPoint> comparison = usages.stream().map(u -> new ComparisonPoint(u.flatNumber(), u.consumption(), avg)).toList();

        List<BillingCycle> cycles = cycleRepository.findAllByOrderByStartDateDesc();
        CycleResponse current = cycles.stream().filter(c -> c.getStatus() == CycleStatus.OPEN).findFirst()
                .or(() -> cycles.stream().findFirst()).map(billingService::cycleResponse).orElse(null);

        List<AlertResponse> anomalies = alertRepository
                .findTop10ByAlertTypeInOrderByIdDesc(List.of(AlertType.ABNORMAL_USAGE, AlertType.POTENTIAL_LEAKAGE))
                .stream().map(Mappers::toAlert).collect(Collectors.toList());

        return new AdminDashboard(households.size(), sum(logs), purchased, invoiceRepository.sumAllTotals(),
                current, usages.stream().limit(5).toList(), anomalies, comparison);
    }

    private BigDecimal sum(List<WaterUsageLog> logs) {
        return logs.stream().map(WaterUsageLog::getConsumption).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total consumption divided by the number of households that reported readings. */
    private BigDecimal averagePerHousehold(List<WaterUsageLog> logs) {
        long reporting = logs.stream().map(l -> l.getHousehold().getId()).distinct().count();
        return reporting == 0 ? BigDecimal.ZERO : sum(logs).divide(BigDecimal.valueOf(reporting), 2, RoundingMode.HALF_UP);
    }
}
