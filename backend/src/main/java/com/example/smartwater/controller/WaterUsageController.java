package com.example.smartwater.controller;

import com.example.smartwater.dto.UsageDtos.*;
import com.example.smartwater.service.WaterUsageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/water-usage")
@RequiredArgsConstructor
@Tag(name = "Water usage", description = "Meter readings, CSV upload and consumption statistics")
public class WaterUsageController {
    private final WaterUsageService service;

    @Operation(summary = "Record a manual daily meter reading (ADMIN)",
            description = "Validates household/meter/date/reading, rejects negative readings, readings lower than the previous one and duplicates (409). Consumption = reading - previous reading.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UsageLogResponse> record(@Valid @RequestBody WaterUsageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordReading(req));
    }

    @Operation(summary = "Bulk CSV upload (ADMIN)", description = "multipart/form-data, field 'file'. Columns: meterId,householdId,readingDate,meterReading. Returns totals, failures, duplicates and per-row errors.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public CsvUploadResult upload(@RequestPart("file") MultipartFile file) { return service.uploadCsv(file); }

    @Operation(summary = "Daily consumption series", description = "RESIDENT: own household. ADMIN: pick householdId or omit for the whole apartment. Defaults to the last 30 days.")
    @GetMapping("/daily")
    public List<DailyPoint> daily(@RequestParam(required = false) Long householdId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.daily(householdId, from, to);
    }

    @Operation(summary = "Monthly consumption series", description = "Defaults to the last 12 months.")
    @GetMapping("/monthly")
    public List<MonthlyPoint> monthly(@RequestParam(required = false) Long householdId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.monthly(householdId, from, to);
    }

    @Operation(summary = "Reading history of a household (ADMIN or that household's resident)", description = "Defaults to the last 90 days.")
    @GetMapping("/{householdId}")
    public List<UsageLogResponse> history(@PathVariable Long householdId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.history(householdId, from, to);
    }
}
