package com.example.smartwater.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tariff_tiers")
@Getter
@Setter
@NoArgsConstructor
public class TariffTier extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    private TariffPlan tariffPlan;

    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;

    @Column(name = "from_kl", nullable = false, precision = 12, scale = 3)
    private BigDecimal fromKl;

    @Column(name = "to_kl", precision = 12, scale = 3)
    private BigDecimal toKl;

    @Column(name = "rate_per_kl", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerKl;
}
