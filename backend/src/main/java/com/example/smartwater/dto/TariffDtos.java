package com.example.smartwater.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TariffDtos {
    private TariffDtos() {}

    public record TierRequest(
            @NotNull @PositiveOrZero BigDecimal fromKl,
            @PositiveOrZero BigDecimal toKl,
            @NotNull @PositiveOrZero BigDecimal ratePerKl) {}

    public record TariffRequest(
            @NotNull Long apartmentId,
            @NotBlank @Size(max = 120) String name,
            @NotNull LocalDate effectiveFrom,
            Boolean active,
            @NotEmpty @Valid List<TierRequest> tiers) {}

    public record TierResponse(Long id, Integer tierOrder, BigDecimal fromKl, BigDecimal toKl, BigDecimal ratePerKl) {}

    public record TariffResponse(Long id, Long apartmentId, String name, LocalDate effectiveFrom,
                                 boolean active, List<TierResponse> tiers) {}
}
