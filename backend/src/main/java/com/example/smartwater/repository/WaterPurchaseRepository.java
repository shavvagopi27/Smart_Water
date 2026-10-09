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

public interface WaterPurchaseRepository extends JpaRepository<WaterPurchase, Long> {
    List<WaterPurchase> findAllByOrderByPurchaseDateDesc();
    List<WaterPurchase> findByApartmentIdOrderByPurchaseDateDesc(Long apartmentId);

    /** Purchases explicitly linked to the cycle, plus unlinked ones dated inside the cycle. */
    @Query("select p from WaterPurchase p where p.apartment.id = :aid and (p.billingCycle.id = :cid or (p.billingCycle is null and p.purchaseDate between :start and :end))")
    List<WaterPurchase> findForCycle(@Param("aid") Long apartmentId, @Param("cid") Long cycleId,
                                     @Param("start") LocalDate start, @Param("end") LocalDate end);
}
