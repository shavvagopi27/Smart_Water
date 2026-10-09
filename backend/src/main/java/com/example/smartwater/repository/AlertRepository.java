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

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByOrderByIdDesc();
    List<Alert> findByHouseholdIdOrderByIdDesc(Long householdId);
    List<Alert> findTop10ByAlertTypeInOrderByIdDesc(Collection<AlertType> types);
    boolean existsByHouseholdIdAndAlertTypeAndUsageDate(Long householdId, AlertType type, LocalDate usageDate);
    long countByHouseholdIdAndReadFlagFalse(Long householdId);
}
