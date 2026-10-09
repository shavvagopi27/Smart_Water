package com.example.smartwater.repository;

import com.example.smartwater.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    List<Household> findByApartmentIdOrderByFlatNumberAsc(Long apartmentId);
    List<Household> findByApartmentIdAndActiveTrueOrderByFlatNumberAsc(Long apartmentId);
    List<Household> findAllByOrderByApartmentIdAscFlatNumberAsc();
    List<Household> findByActiveTrue();
    boolean existsByApartmentIdAndFlatNumberIgnoreCase(Long apartmentId, String flatNumber);
    boolean existsByApartmentId(Long apartmentId);
}
