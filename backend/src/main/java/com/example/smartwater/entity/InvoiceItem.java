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
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private InvoiceItemType itemType;

    @Column(nullable = false)
    private String description;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
