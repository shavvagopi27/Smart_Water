package com.example.smartwater.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class HouseholdDtos {
    private HouseholdDtos() {}

    public record HouseholdRequest(
            @NotNull Long apartmentId,
            @NotBlank @Size(max = 20) String flatNumber,
            @NotNull @Positive Integer householdSize,
            @NotNull @Positive BigDecimal areaSqft,
            Boolean active) {}

    public record HouseholdResponse(Long id, Long apartmentId, String apartmentName, String flatNumber,
                                    Integer householdSize, BigDecimal areaSqft, boolean active,
                                    Long meterId, String meterNumber, List<String> residentNames) {}
}
