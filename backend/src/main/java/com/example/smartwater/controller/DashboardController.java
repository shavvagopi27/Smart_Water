package com.example.smartwater.controller;

import com.example.smartwater.dto.DashboardDtos.*;
import com.example.smartwater.service.DashboardService;
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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards")
public class DashboardController {
    private final DashboardService service;

    @Operation(summary = "Resident dashboard data (RESIDENT)", description = "400 when the resident is not assigned to a household yet.")
    @PreAuthorize("hasRole('RESIDENT')")
    @GetMapping("/resident")
    public ResidentDashboard resident() { return service.resident(); }

    @Operation(summary = "Admin dashboard data (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public AdminDashboard admin() { return service.admin(); }
}
