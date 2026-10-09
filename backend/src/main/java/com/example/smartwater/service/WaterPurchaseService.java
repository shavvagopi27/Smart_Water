package com.example.smartwater.service;

import com.example.smartwater.dto.PurchaseDtos.*;
import com.example.smartwater.entity.Apartment;
import com.example.smartwater.entity.BillingCycle;
import com.example.smartwater.entity.CycleStatus;
import com.example.smartwater.entity.WaterPurchase;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.BillingCycleRepository;
import com.example.smartwater.repository.WaterPurchaseRepository;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaterPurchaseService {
    private final WaterPurchaseRepository purchaseRepository;
    private final BillingCycleRepository cycleRepository;
    private final ApartmentService apartmentService;

    @Transactional
    public PurchaseResponse create(PurchaseRequest req) {
        Apartment apartment = apartmentService.get(req.apartmentId());
        WaterPurchase p = new WaterPurchase();
        p.setApartment(apartment);
        apply(p, req);
        return Mappers.toPurchase(purchaseRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> findAll(Long apartmentId) {
        List<WaterPurchase> list = apartmentId == null ? purchaseRepository.findAllByOrderByPurchaseDateDesc()
                : purchaseRepository.findByApartmentIdOrderByPurchaseDateDesc(apartmentId);
        return list.stream().map(Mappers::toPurchase).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse findById(Long id) {
        return Mappers.toPurchase(get(id));
    }

    @Transactional
    public PurchaseResponse update(Long id, PurchaseRequest req) {
        WaterPurchase p = get(id);
        if (!p.getApartment().getId().equals(req.apartmentId())) {
            throw new BadRequestException("A purchase cannot be moved to another apartment");
        }
        assertPeriodOpen(p.getApartment().getId(), p.getPurchaseDate(), p.getBillingCycle()); // existing data
        apply(p, req);
        return Mappers.toPurchase(purchaseRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        WaterPurchase p = get(id);
        assertPeriodOpen(p.getApartment().getId(), p.getPurchaseDate(), p.getBillingCycle());
        purchaseRepository.delete(p);
    }

    public WaterPurchase get(Long id) {
        return purchaseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Water purchase not found"));
    }

    private void apply(WaterPurchase p, PurchaseRequest req) {
        BillingCycle cycle = null;
        if (req.billingCycleId() != null) {
            cycle = cycleRepository.findById(req.billingCycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Billing cycle not found"));
            if (!cycle.getApartment().getId().equals(p.getApartment().getId())) {
                throw new BadRequestException("Billing cycle belongs to a different apartment");
            }
        }
        assertPeriodOpen(p.getApartment().getId(), req.purchaseDate(), cycle); // new data
        p.setBillingCycle(cycle);
        p.setSupplier(req.supplier().trim());
        p.setSupplyType(req.supplyType());
        p.setPurchaseDate(req.purchaseDate());
        p.setVolumeKl(req.volumeKl());
        p.setUnitCost(req.unitCost());
        p.setTotalCost(req.totalCost() != null ? req.totalCost().setScale(2, RoundingMode.HALF_UP)
                : req.volumeKl().multiply(req.unitCost()).setScale(2, RoundingMode.HALF_UP));
    }

    /** Finalized/archived cycles must not change because of purchase edits. */
    private void assertPeriodOpen(Long apartmentId, LocalDate date, BillingCycle linked) {
        if (linked != null && linked.getStatus() != CycleStatus.OPEN) {
            throw new ConflictException("Billing cycle '" + linked.getName() + "' is " + linked.getStatus() + " and cannot be modified");
        }
        if (linked == null && cycleRepository.existsByApartmentIdAndStatusNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                apartmentId, CycleStatus.OPEN, date, date)) {
            throw new ConflictException("The purchase date falls inside a finalized or archived billing cycle");
        }
    }
}
