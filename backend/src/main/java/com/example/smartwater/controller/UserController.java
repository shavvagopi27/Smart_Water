package com.example.smartwater.controller;

import com.example.smartwater.dto.UserDtos.*;
import com.example.smartwater.entity.Role;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {
    private final UserService service;

    @Operation(summary = "List users (ADMIN)", description = "Optional ?role=RESIDENT|ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) { return service.listUsers(role); }

    @Operation(summary = "Get user (ADMIN, or yourself)")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { return service.getUser(id); }

    @Operation(summary = "Update user (ADMIN any user; resident only own name/phone)")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) { return service.updateUser(id, req); }
}
