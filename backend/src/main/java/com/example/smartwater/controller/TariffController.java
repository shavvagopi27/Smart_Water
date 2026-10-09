package com.example.smartwater.controller;

import com.example.smartwater.dto.TariffDtos.*;
import com.example.smartwater.service.TariffService;
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
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Tariffs", description = "Configurable tiered tariffs (ADMIN only)")
public class TariffController {
    private final TariffService service;

    @Operation(summary = "Create tariff plan", description = "Tiers must start at 0, be contiguous and end with one open-ended tier (toKl null). Activating a plan deactivates the apartment's other plans.")
    @PostMapping
    public ResponseEntity<TariffResponse> create(@Valid @RequestBody TariffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "List tariff plans")
    @GetMapping
    public List<TariffResponse> list(@RequestParam(required = false) Long apartmentId) { return service.findAll(apartmentId); }

    @Operation(summary = "Get tariff plan")
    @GetMapping("/{id}")
    public TariffResponse get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Update tariff plan (replaces tiers)")
    @PutMapping("/{id}")
    public TariffResponse update(@PathVariable Long id, @Valid @RequestBody TariffRequest req) { return service.update(id, req); }

    @Operation(summary = "Delete tariff plan")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
