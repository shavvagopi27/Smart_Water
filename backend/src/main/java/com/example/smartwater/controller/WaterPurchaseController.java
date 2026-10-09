package com.example.smartwater.controller;

import com.example.smartwater.dto.PurchaseDtos.*;
import com.example.smartwater.service.WaterPurchaseService;
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
@RequestMapping("/api/water-purchases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Water purchases", description = "Bulk tanker / municipal purchases (ADMIN only)")
public class WaterPurchaseController {
    private final WaterPurchaseService service;

    @Operation(summary = "Record a purchase", description = "totalCost defaults to volumeKl x unitCost. 409 if the date/cycle is already finalized.")
    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "List purchases")
    @GetMapping
    public List<PurchaseResponse> list(@RequestParam(required = false) Long apartmentId) { return service.findAll(apartmentId); }

    @Operation(summary = "Get purchase")
    @GetMapping("/{id}")
    public PurchaseResponse get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Update purchase")
    @PutMapping("/{id}")
    public PurchaseResponse update(@PathVariable Long id, @Valid @RequestBody PurchaseRequest req) { return service.update(id, req); }

    @Operation(summary = "Delete purchase")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
