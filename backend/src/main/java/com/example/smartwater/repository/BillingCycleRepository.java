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

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {
    List<BillingCycle> findAllByOrderByStartDateDesc();
    List<BillingCycle> findByApartmentIdOrderByStartDateDesc(Long apartmentId);
    boolean existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long apartmentId, LocalDate end, LocalDate start);
    /** True when a cycle that is NOT in the given status covers the date (used to lock closed periods). */
    boolean existsByApartmentIdAndStatusNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long apartmentId, CycleStatus status, LocalDate date1, LocalDate date2);
    Optional<BillingCycle> findFirstByApartmentIdAndStatusOrderByStartDateDesc(Long apartmentId, CycleStatus status);
}
