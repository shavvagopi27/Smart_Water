package com.example.smartwater.service;

import com.example.smartwater.dto.ApartmentDtos.*;
import com.example.smartwater.entity.Apartment;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.ApartmentRepository;
import com.example.smartwater.repository.HouseholdRepository;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentService {
    private final ApartmentRepository apartmentRepository;
    private final HouseholdRepository householdRepository;

    @Transactional
    public ApartmentResponse create(ApartmentRequest req) {
        Apartment a = new Apartment();
        apply(a, req);
        return Mappers.toApartment(apartmentRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> findAll() {
        return apartmentRepository.findAll().stream().map(Mappers::toApartment).toList();
    }

    /** Admins can read any apartment; residents only the apartment they live in. */
    @Transactional(readOnly = true)
    public ApartmentResponse findById(Long id) {
        AuthUser user = SecurityUtils.currentUser();
        if (!user.isAdmin() && !id.equals(user.apartmentId())) {
            throw new AccessDeniedException("You can only view your own apartment");
        }
        return Mappers.toApartment(get(id));
    }

    @Transactional
    public ApartmentResponse update(Long id, ApartmentRequest req) {
        Apartment a = get(id);
        apply(a, req);
        return Mappers.toApartment(apartmentRepository.save(a));
    }

    @Transactional
    public void delete(Long id) {
        Apartment a = get(id);
        if (householdRepository.existsByApartmentId(id)) {
            throw new ConflictException("Cannot delete an apartment that still has households");
        }
        apartmentRepository.delete(a);
        apartmentRepository.flush();
    }

    public Apartment get(Long id) {
        return apartmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));
    }

    private void apply(Apartment a, ApartmentRequest req) {
        a.setName(req.name().trim());
        a.setAddress(req.address().trim());
        a.setCity(req.city().trim());
    }
}
