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
@Table(name = "water_purchases")
@Getter
@Setter
@NoArgsConstructor
public class WaterPurchase extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_cycle_id")
    private BillingCycle billingCycle;

    @Column(nullable = false, length = 150)
    private String supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_type", nullable = false, length = 20)
    private SupplyType supplyType;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "volume_kl", nullable = false, precision = 12, scale = 3)
    private BigDecimal volumeKl;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;
}
