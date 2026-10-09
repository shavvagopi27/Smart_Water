package com.example.smartwater.dto;

import com.example.smartwater.entity.Role;
import jakarta.validation.constraints.*;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(Long id, String name, String email, String phone, Role role,
                               Long apartmentId, Long householdId, String flatNumber, boolean enabled) {}

    /** Admins may change every field; residents may only change name and phone. */
    public record UpdateUserRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 30) String phone,
            Long householdId,
            Boolean enabled) {}

    /** Adds a resident to a household. Creates the user if the e-mail is new. */
    public record ResidentRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @Size(min = 8, max = 72) String password,
            @Size(max = 30) String phone) {}
}
