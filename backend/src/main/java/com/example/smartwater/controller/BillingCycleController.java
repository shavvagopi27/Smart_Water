package com.example.smartwater.controller;

import com.example.smartwater.dto.BillingDtos.*;
import com.example.smartwater.service.BillingService;
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
@RequestMapping("/api/billing-cycles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Billing cycles", description = "OPEN -> FINALIZED -> ARCHIVED (ADMIN only)")
public class BillingCycleController {
    private final BillingService billingService;

    @Operation(summary = "Create a billing cycle (status OPEN)", description = "409 if it overlaps an existing cycle of the apartment.")
    @PostMapping
    public ResponseEntity<CycleResponse> create(@Valid @RequestBody CycleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createCycle(req));
    }

    @Operation(summary = "List billing cycles")
    @GetMapping
    public List<CycleResponse> list(@RequestParam(required = false) Long apartmentId) { return billingService.listCycles(apartmentId); }

    @Operation(summary = "Get billing cycle")
    @GetMapping("/{id}")
    public CycleResponse get(@PathVariable Long id) { return billingService.getCycle(id); }

    @Operation(summary = "Generate / regenerate invoices for an OPEN cycle",
            description = "Runs the billing engine: tiered tariff + proportional shared cost + optional adjustments. 409 when the cycle is not OPEN.")
    @PostMapping("/{id}/generate")
    public List<InvoiceResponse> generate(@PathVariable Long id, @Valid @RequestBody(required = false) GenerateRequest req) {
        return billingService.generateInvoices(id, req);
    }

    @Operation(summary = "Finalize the cycle", description = "Locks the cycle, generates invoices if none exist and e-mails the bills.")
    @PutMapping("/{id}/finalize")
    public CycleResponse finalizeCycle(@PathVariable Long id) { return billingService.finalizeCycle(id); }

    @Operation(summary = "Archive a FINALIZED cycle")
    @PutMapping("/{id}/archive")
    public CycleResponse archive(@PathVariable Long id) { return billingService.archiveCycle(id); }
}
