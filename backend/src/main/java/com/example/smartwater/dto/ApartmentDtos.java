package com.example.smartwater.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class ApartmentDtos {
    private ApartmentDtos() {}

    public record ApartmentRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 100) String city) {}

    public record ApartmentResponse(Long id, String name, String address, String city,
                                    int householdCount, Instant createdAt) {}
}
