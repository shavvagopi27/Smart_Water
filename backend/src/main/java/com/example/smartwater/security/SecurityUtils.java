package com.example.smartwater.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static AuthUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new AccessDeniedException("Not authenticated");
        }
        return user;
    }

    /** Admins may access any household; residents only the one they belong to. */
    public static void requireHouseholdAccess(Long householdId) {
        AuthUser user = currentUser();
        if (user.isAdmin()) return;
        if (householdId == null || !householdId.equals(user.householdId())) {
            throw new AccessDeniedException("You can only access your own household");
        }
    }
}
