package com.example.smartwater.controller;

import com.example.smartwater.dto.ApartmentDtos.*;
import com.example.smartwater.service.ApartmentService;
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
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
@Tag(name = "Apartments")
public class ApartmentController {
    private final ApartmentService service;

    @Operation(summary = "Create apartment (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApartmentResponse> create(@Valid @RequestBody ApartmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "List apartments (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<ApartmentResponse> list() { return service.findAll(); }

    @Operation(summary = "Get apartment (ADMIN, or a resident of that apartment)")
    @GetMapping("/{id}")
    public ApartmentResponse get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Update apartment (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApartmentResponse update(@PathVariable Long id, @Valid @RequestBody ApartmentRequest req) { return service.update(id, req); }

    @Operation(summary = "Delete apartment (ADMIN)", description = "409 if the apartment still has households.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
