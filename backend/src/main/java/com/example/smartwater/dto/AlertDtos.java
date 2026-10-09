package com.example.smartwater.dto;

import com.example.smartwater.entity.*;
import java.time.Instant;
import java.time.LocalDate;

public final class AlertDtos {
    private AlertDtos() {}

    public record AlertResponse(Long id, Long householdId, String flatNumber, AlertType alertType,
                                AlertSeverity severity, String message, LocalDate usageDate,
                                boolean read, Instant createdAt) {}
}
