package com.example.smartwater.controller;

import com.example.smartwater.dto.TipDtos.*;
import com.example.smartwater.service.TipService;
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
@RequestMapping("/api/tips")
@RequiredArgsConstructor
@Tag(name = "Water-saving tips")
public class TipController {
    private final TipService service;

    @Operation(summary = "List water-saving tips (any signed-in user)")
    @GetMapping
    public List<TipResponse> list() { return service.findAll(); }

    @Operation(summary = "Create tip (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TipResponse> create(@Valid @RequestBody TipRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "Update tip (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public TipResponse update(@PathVariable Long id, @Valid @RequestBody TipRequest req) { return service.update(id, req); }

    @Operation(summary = "Delete tip (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
