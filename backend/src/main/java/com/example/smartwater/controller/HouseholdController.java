package com.example.smartwater.controller;

import com.example.smartwater.dto.HouseholdDtos.*;
import com.example.smartwater.dto.UserDtos.*;
import com.example.smartwater.service.HouseholdService;
import com.example.smartwater.service.UserService;
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
@RequestMapping("/api/households")
@RequiredArgsConstructor
@Tag(name = "Households & residents")
public class HouseholdController {
    private final HouseholdService service;
    private final UserService userService;

    @Operation(summary = "Create household (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HouseholdResponse> create(@Valid @RequestBody HouseholdRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "List households", description = "ADMIN: all (optional apartmentId filter). RESIDENT: only their own household.")
    @GetMapping
    public List<HouseholdResponse> list(@RequestParam(required = false) Long apartmentId) { return service.findAll(apartmentId); }

    @Operation(summary = "Get household (ADMIN or its resident)")
    @GetMapping("/{id}")
    public HouseholdResponse get(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Update household (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public HouseholdResponse update(@PathVariable Long id, @Valid @RequestBody HouseholdRequest req) { return service.update(id, req); }

    @Operation(summary = "Delete household (ADMIN)", description = "409 if invoices exist for the household.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a resident to a household (ADMIN)", description = "Creates the account if the email is new (password required), otherwise links the existing resident.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{householdId}/residents")
    public ResponseEntity<UserResponse> addResident(@PathVariable Long householdId, @Valid @RequestBody ResidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addResident(householdId, req));
    }

    @Operation(summary = "List residents of a household (ADMIN or that household's residents)")
    @GetMapping("/{householdId}/residents")
    public List<UserResponse> residents(@PathVariable Long householdId) { return userService.listResidents(householdId); }

    @Operation(summary = "Remove a resident from a household (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{householdId}/residents/{userId}")
    public ResponseEntity<Void> removeResident(@PathVariable Long householdId, @PathVariable Long userId) {
        userService.removeResident(householdId, userId);
        return ResponseEntity.noContent().build();
    }
}
