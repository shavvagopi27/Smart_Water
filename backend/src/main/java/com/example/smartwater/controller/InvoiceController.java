package com.example.smartwater.controller;

import com.example.smartwater.dto.BillingDtos.InvoiceResponse;
import com.example.smartwater.service.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices")
public class InvoiceController {
    private final InvoiceService service;

    @Operation(summary = "List invoices", description = "ADMIN: all (filter by cycleId/householdId). RESIDENT: only their household's invoices.")
    @GetMapping
    public List<InvoiceResponse> list(@RequestParam(required = false) Long cycleId, @RequestParam(required = false) Long householdId) {
        return service.findAll(cycleId, householdId);
    }

    @Operation(summary = "Get invoice (ADMIN or owning resident)")
    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Download invoice PDF (ADMIN or owning resident)")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        InvoiceService.PdfFile file = service.pdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.content());
    }

    @Operation(summary = "Mark invoice as paid (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/paid")
    public InvoiceResponse markPaid(@PathVariable Long id) { return service.markPaid(id); }

    @Operation(summary = "E-mail the invoice PDF to the household's residents (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/email")
    public ResponseEntity<Void> email(@PathVariable Long id) {
        service.resendEmail(id);
        return ResponseEntity.accepted().build();
    }
}
