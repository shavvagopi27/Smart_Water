package com.example.smartwater.service;

import com.example.smartwater.dto.TariffDtos.*;
import com.example.smartwater.entity.Apartment;
import com.example.smartwater.entity.TariffPlan;
import com.example.smartwater.entity.TariffTier;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.TariffPlanRepository;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffService {
    private final TariffPlanRepository tariffRepository;
    private final ApartmentService apartmentService;

    @Transactional
    public TariffResponse create(TariffRequest req) {
        Apartment apartment = apartmentService.get(req.apartmentId());
        List<TierRequest> tiers = validated(req);
        TariffPlan plan = new TariffPlan();
        plan.setApartment(apartment);
        plan.setName(req.name().trim());
        plan.setEffectiveFrom(req.effectiveFrom());
        plan.setActive(req.active() == null || req.active());
        applyTiers(plan, tiers);
        if (plan.isActive()) deactivateOthers(apartment.getId(), null);
        return Mappers.toTariff(tariffRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> findAll(Long apartmentId) {
        List<TariffPlan> plans = apartmentId == null ? tariffRepository.findAllByOrderByIdDesc()
                : tariffRepository.findByApartmentIdOrderByIdDesc(apartmentId);
        return plans.stream().map(Mappers::toTariff).toList();
    }

    @Transactional(readOnly = true)
    public TariffResponse findById(Long id) {
        return Mappers.toTariff(get(id));
    }

    @Transactional
    public TariffResponse update(Long id, TariffRequest req) {
        TariffPlan plan = get(id);
        if (!plan.getApartment().getId().equals(req.apartmentId())) {
            throw new BadRequestException("A tariff cannot be moved to another apartment");
        }
        List<TierRequest> tiers = validated(req);
        plan.setName(req.name().trim());
        plan.setEffectiveFrom(req.effectiveFrom());
        plan.setActive(req.active() == null || req.active());
        applyTiers(plan, tiers);
        if (plan.isActive()) deactivateOthers(plan.getApartment().getId(), plan.getId());
        return Mappers.toTariff(tariffRepository.save(plan));
    }

    @Transactional
    public void delete(Long id) {
        tariffRepository.delete(get(id));
    }

    public TariffPlan get(Long id) {
        return tariffRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tariff plan not found"));
    }

    private List<TierRequest> validated(TariffRequest req) {
        List<TierRequest> sorted = req.tiers().stream().sorted(Comparator.comparing(TierRequest::fromKl)).toList();
        TariffCalculator.validate(sorted.stream()
                .map(t -> new TariffCalculator.TierDef(0, t.fromKl(), t.toKl(), t.ratePerKl())).toList());
        return sorted;
    }

    /** Updates existing tier rows in place (avoids unique-order clashes), then adds/removes the difference. */
    private void applyTiers(TariffPlan plan, List<TierRequest> tiers) {
        List<TariffTier> existing = plan.getTiers();
        for (int i = 0; i < tiers.size(); i++) {
            TariffTier t = i < existing.size() ? existing.get(i) : new TariffTier();
            t.setTariffPlan(plan);
            t.setTierOrder(i + 1);
            t.setFromKl(tiers.get(i).fromKl());
            t.setToKl(tiers.get(i).toKl());
            t.setRatePerKl(tiers.get(i).ratePerKl());
            if (i >= existing.size()) existing.add(t);
        }
        while (existing.size() > tiers.size()) {
            existing.remove(existing.size() - 1);
        }
    }

    private void deactivateOthers(Long apartmentId, Long keepId) {
        for (TariffPlan other : tariffRepository.findByApartmentIdAndActiveTrue(apartmentId)) {
            if (!other.getId().equals(keepId)) {
                other.setActive(false);
                tariffRepository.save(other);
            }
        }
    }
}
