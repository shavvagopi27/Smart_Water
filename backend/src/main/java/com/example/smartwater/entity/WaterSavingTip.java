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
@Table(name = "water_saving_tips")
@Getter
@Setter
@NoArgsConstructor
public class WaterSavingTip extends BaseEntity {
    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;
}
