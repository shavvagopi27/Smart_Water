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
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseEntity {
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_cycle_id", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "resident_name", nullable = false)
    private String residentName;

    @Column(name = "previous_reading", nullable = false, precision = 12, scale = 3)
    private BigDecimal previousReading;

    @Column(name = "current_reading", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentReading;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal consumption;

    @Column(name = "water_charge", nullable = false, precision = 14, scale = 2)
    private BigDecimal waterCharge;

    @Column(name = "shared_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal sharedCost;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal adjustment = BigDecimal.ZERO;

    @Column(name = "adjustment_reason")
    private String adjustmentReason;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "shared_cost_basis", nullable = false, length = 20)
    private SharedCostBasis sharedCostBasis = SharedCostBasis.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<InvoiceItem> items = new ArrayList<>();
}
