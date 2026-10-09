package com.example.smartwater.dto;

import com.example.smartwater.entity.MeterStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class MeterDtos {
    private MeterDtos() {}

    public record MeterRequest(
            @NotBlank @Size(max = 50) String meterNumber,
            @NotNull Long householdId,
            @NotNull @PastOrPresent LocalDate installationDate,
            MeterStatus status,
            @NotNull @PositiveOrZero BigDecimal initialReading) {}

    public record MeterResponse(Long id, String meterNumber, Long householdId, String flatNumber,
                                LocalDate installationDate, MeterStatus status,
                                BigDecimal initialReading, BigDecimal currentReading) {}
}
