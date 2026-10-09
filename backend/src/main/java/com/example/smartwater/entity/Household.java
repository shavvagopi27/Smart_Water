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
@Table(name = "households")
@Getter
@Setter
@NoArgsConstructor
public class Household extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "flat_number", nullable = false, length = 20)
    private String flatNumber;

    @Column(name = "household_size", nullable = false)
    private Integer householdSize;

    @Column(name = "area_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaSqft;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "household", fetch = FetchType.LAZY)
    private WaterMeter meter;

    @OneToMany(mappedBy = "household")
    private List<User> residents = new ArrayList<>();
}
