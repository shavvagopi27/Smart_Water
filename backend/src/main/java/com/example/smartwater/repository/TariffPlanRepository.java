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

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {
    List<TariffPlan> findAllByOrderByIdDesc();
    List<TariffPlan> findByApartmentIdOrderByIdDesc(Long apartmentId);
    List<TariffPlan> findByApartmentIdAndActiveTrue(Long apartmentId);
}
