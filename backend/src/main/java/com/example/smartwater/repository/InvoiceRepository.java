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

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIdDesc();
    List<Invoice> findByHouseholdIdOrderByIdDesc(Long householdId);
    List<Invoice> findByBillingCycleIdOrderByHouseholdIdAsc(Long cycleId);
    long countByBillingCycleId(Long cycleId);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i")
    BigDecimal sumAllTotals();

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.billingCycle.id = :cid")
    BigDecimal sumTotalsByCycle(@Param("cid") Long cycleId);
}
