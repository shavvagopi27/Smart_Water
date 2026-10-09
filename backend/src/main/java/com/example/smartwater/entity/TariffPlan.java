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
@Table(name = "tariff_plans")
@Getter
@Setter
@NoArgsConstructor
public class TariffPlan extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "tariffPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tierOrder ASC")
    private List<TariffTier> tiers = new ArrayList<>();
}
