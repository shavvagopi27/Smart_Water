package com.example.smartwater.dto;

import jakarta.validation.constraints.*;

public final class TipDtos {
    private TipDtos() {}

    public record TipRequest(@NotBlank @Size(max = 150) String title, @NotBlank @Size(max = 1000) String content) {}

    public record TipResponse(Long id, String title, String content) {}
}
