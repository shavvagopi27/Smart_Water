package com.example.smartwater.controller;

import com.example.smartwater.dto.AlertDtos.AlertResponse;
import com.example.smartwater.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts")
public class AlertController {
    private final AlertService service;

    @Operation(summary = "List alerts", description = "ADMIN: all (optional householdId). RESIDENT: own household only.")
    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) Long householdId) { return service.list(householdId); }

    @Operation(summary = "Mark an alert as read")
    @PutMapping("/{id}/read")
    public AlertResponse markRead(@PathVariable Long id) { return service.markRead(id); }

    @Operation(summary = "Run anomaly/threshold detection now (ADMIN)", description = "The same job that runs daily via @Scheduled.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/run-detection")
    public Map<String, Integer> runDetection() { return Map.of("newAlerts", service.runDetection()); }
}
