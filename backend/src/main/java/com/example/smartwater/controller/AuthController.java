package com.example.smartwater.controller;

import com.example.smartwater.dto.AuthDtos.*;
import com.example.smartwater.dto.UserDtos.UserResponse;
import com.example.smartwater.service.AuthService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and current user")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Register a new resident account", description = "Public. Always creates a RESIDENT; an admin assigns the household afterwards. Errors: 400 validation, 409 email exists.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(summary = "Login", description = "Public. Returns a JWT and the user. Errors: 400 validation, 401 bad credentials.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Current authenticated user")
    @GetMapping("/me")
    public UserResponse me() {
        return authService.me();
    }
}
