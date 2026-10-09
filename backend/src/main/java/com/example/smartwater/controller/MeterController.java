package com.example.smartwater.controller;

import com.example.smartwater.dto.MeterDtos.*;
import com.example.smartwater.service.MeterService;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "Water meters")
public class MeterController {
    private final MeterService service;

    @Operation(summary = "Create meter (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MeterResponse> create(@Valid @RequestBody MeterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "List meters", description = "ADMIN: all. RESIDENT: own household's meter.")
    @GetMapping
    public List<MeterResponse> list() { return service.findAll(); }

    @Operation(summary = "Get meter (ADMIN or owning resident)")
    @GetMapping("/{id}")
    public MeterResponse get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Update meter (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public MeterResponse update(@PathVariable Long id, @Valid @RequestBody MeterRequest req) { return service.update(id, req); }
}
