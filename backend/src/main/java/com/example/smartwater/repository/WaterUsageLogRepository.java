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

public interface WaterUsageLogRepository extends JpaRepository<WaterUsageLog, Long> {
    Optional<WaterUsageLog> findTopByMeterIdOrderByReadingDateDesc(Long meterId);
    Optional<WaterUsageLog> findTopByHouseholdIdOrderByReadingDateDesc(Long householdId);
    boolean existsByMeterIdAndReadingDate(Long meterId, LocalDate readingDate);
    boolean existsByMeterId(Long meterId);
    List<WaterUsageLog> findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(Long householdId, LocalDate from, LocalDate to);
    List<WaterUsageLog> findByHouseholdIdInAndReadingDateBetweenOrderByReadingDateAsc(Collection<Long> householdIds, LocalDate from, LocalDate to);
}
