package com.example.smartwater.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 120) @Schema(example = "Asha Verma") String name,
            @NotBlank @Email @Size(max = 160) @Schema(example = "asha@example.com") String email,
            @NotBlank @Size(min = 8, max = 72) @Schema(example = "StrongPass#123") String password,
            @Size(max = 30) @Schema(example = "+91-9876543210") String phone) {}

    public record LoginRequest(
            @NotBlank @Email @Schema(example = "admin@smartwater.local") String email,
            @NotBlank @Schema(example = "Admin@12345") String password) {}

    public record AuthResponse(String token, UserDtos.UserResponse user) {}
}
