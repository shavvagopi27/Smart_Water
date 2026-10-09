package com.example.smartwater.dto;

import com.example.smartwater.entity.SupplyType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PurchaseDtos {
    private PurchaseDtos() {}

    public record PurchaseRequest(
            @NotNull Long apartmentId,
            Long billingCycleId,
            @NotBlank @Size(max = 150) String supplier,
            @NotNull SupplyType supplyType,
            @NotNull LocalDate purchaseDate,
            @NotNull @Positive BigDecimal volumeKl,
            @NotNull @PositiveOrZero BigDecimal unitCost,
            @PositiveOrZero BigDecimal totalCost) {}

    public record PurchaseResponse(Long id, Long apartmentId, Long billingCycleId, String supplier,
                                   SupplyType supplyType, LocalDate purchaseDate, BigDecimal volumeKl,
                                   BigDecimal unitCost, BigDecimal totalCost) {}
}
