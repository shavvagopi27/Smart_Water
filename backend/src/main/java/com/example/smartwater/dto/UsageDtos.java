package com.example.smartwater.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.smartwater.entity.ReadingSource;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class UsageDtos {
    private UsageDtos() {}

    public record WaterUsageRequest(
            @NotNull @Schema(example = "1") Long householdId,
            @NotNull @Schema(example = "1") Long meterId,
            @NotNull @Schema(example = "2026-10-07") LocalDate readingDate,
            @NotNull @PositiveOrZero @Schema(example = "1215.000") BigDecimal meterReading) {}

    public record UsageLogResponse(Long id, Long householdId, String flatNumber, Long meterId, String meterNumber,
                                   LocalDate readingDate, BigDecimal meterReading, BigDecimal consumption,
                                   ReadingSource source) {}

    public record DailyPoint(LocalDate date, BigDecimal consumption) {}

    public record MonthlyPoint(String month, BigDecimal consumption) {}

    public record RowError(int row, String message) {}

    public record CsvUploadResult(int totalRecords, int successfulRecords, int failedRecords,
                                  int duplicateRecords, List<RowError> errors) {}
}
