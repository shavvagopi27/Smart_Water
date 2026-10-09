package com.example.smartwater.service;

import com.example.smartwater.dto.HouseholdDtos.*;
import com.example.smartwater.entity.Apartment;
import com.example.smartwater.entity.Household;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.HouseholdRepository;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseholdService {
    private final HouseholdRepository householdRepository;
    private final ApartmentService apartmentService;

    @Transactional
    public HouseholdResponse create(HouseholdRequest req) {
        Apartment apartment = apartmentService.get(req.apartmentId());
        if (householdRepository.existsByApartmentIdAndFlatNumberIgnoreCase(apartment.getId(), req.flatNumber().trim())) {
            throw new ConflictException("Flat " + req.flatNumber().trim() + " already exists in this apartment");
        }
        Household h = new Household();
        h.setApartment(apartment);
        apply(h, req);
        return Mappers.toHousehold(householdRepository.save(h));
    }

    /** Admin: all households (optionally filtered by apartment). Resident: only their own household. */
    @Transactional(readOnly = true)
    public List<HouseholdResponse> findAll(Long apartmentId) {
        AuthUser user = SecurityUtils.currentUser();
        if (!user.isAdmin()) {
            if (user.householdId() == null) return List.of();
            return List.of(Mappers.toHousehold(get(user.householdId())));
        }
        List<Household> list = apartmentId == null
                ? householdRepository.findAllByOrderByApartmentIdAscFlatNumberAsc()
                : householdRepository.findByApartmentIdOrderByFlatNumberAsc(apartmentId);
        return list.stream().map(Mappers::toHousehold).toList();
    }

    @Transactional(readOnly = true)
    public HouseholdResponse findById(Long id) {
        SecurityUtils.requireHouseholdAccess(id);
        return Mappers.toHousehold(get(id));
    }

    @Transactional
    public HouseholdResponse update(Long id, HouseholdRequest req) {
        Household h = get(id);
        if (!h.getApartment().getId().equals(req.apartmentId())) {
            throw new BadRequestException("A household cannot be moved to another apartment");
        }
        if (!h.getFlatNumber().equalsIgnoreCase(req.flatNumber().trim())
                && householdRepository.existsByApartmentIdAndFlatNumberIgnoreCase(h.getApartment().getId(), req.flatNumber().trim())) {
            throw new ConflictException("Flat " + req.flatNumber().trim() + " already exists in this apartment");
        }
        apply(h, req);
        return Mappers.toHousehold(householdRepository.save(h));
    }

    @Transactional
    public void delete(Long id) {
        Household h = get(id);
        householdRepository.delete(h);
        householdRepository.flush(); // invoices reference households -> 409 if billed already
    }

    public Household get(Long id) {
        return householdRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Household not found"));
    }

    private void apply(Household h, HouseholdRequest req) {
        h.setFlatNumber(req.flatNumber().trim());
        h.setHouseholdSize(req.householdSize());
        h.setAreaSqft(req.areaSqft());
        h.setActive(req.active() == null || req.active());
    }
}
