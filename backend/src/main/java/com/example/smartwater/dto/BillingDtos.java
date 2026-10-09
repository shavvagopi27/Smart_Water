package com.example.smartwater.dto;

import com.example.smartwater.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BillingDtos {
    private BillingDtos() {}

    public record CycleRequest(
            @NotNull Long apartmentId,
            @NotBlank @Size(max = 120) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate) {}

    public record CycleResponse(Long id, Long apartmentId, String name, LocalDate startDate, LocalDate endDate,
                                CycleStatus status, Instant finalizedAt, long invoiceCount,
                                BigDecimal totalPurchaseCost, BigDecimal totalInvoiced) {}

    public record AdjustmentRequest(
            @NotNull Long householdId,
            @NotNull BigDecimal amount,
            @NotBlank @Size(max = 255) String reason) {}

    public record GenerateRequest(@Valid List<AdjustmentRequest> adjustments) {}

    public record InvoiceItemResponse(InvoiceItemType type, String description, BigDecimal quantity,
                                      BigDecimal rate, BigDecimal amount) {}

    public record InvoiceResponse(Long id, String invoiceNumber, Long householdId, String flatNumber,
                                  String residentName, Long billingCycleId, String cycleName,
                                  LocalDate periodStart, LocalDate periodEnd,
                                  BigDecimal previousReading, BigDecimal currentReading, BigDecimal consumption,
                                  BigDecimal waterCharge, BigDecimal sharedCost, BigDecimal adjustment,
                                  String adjustmentReason, BigDecimal totalAmount, SharedCostBasis sharedCostBasis,
                                  PaymentStatus paymentStatus, Instant generatedAt,
                                  List<InvoiceItemResponse> items, String paymentInstructions) {}
}
