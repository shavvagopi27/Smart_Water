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
@Table(name = "water_meters")
@Getter
@Setter
@NoArgsConstructor
public class WaterMeter extends BaseEntity {
    @Column(name = "meter_number", nullable = false, unique = true, length = 50)
    private String meterNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, unique = true)
    private Household household;

    @Column(name = "installation_date", nullable = false)
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterStatus status = MeterStatus.ACTIVE;

    @Column(name = "initial_reading", nullable = false, precision = 12, scale = 3)
    private BigDecimal initialReading;

    @Column(name = "current_reading", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentReading;
}
