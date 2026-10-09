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
@Table(name = "water_usage_logs")
@Getter
@Setter
@NoArgsConstructor
public class WaterUsageLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private WaterMeter meter;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "meter_reading", nullable = false, precision = 12, scale = 3)
    private BigDecimal meterReading;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal consumption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReadingSource source = ReadingSource.MANUAL;
}
