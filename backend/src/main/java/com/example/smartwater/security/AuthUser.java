package com.example.smartwater.security;

import com.example.smartwater.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Authenticated principal placed in the SecurityContext for each request. */
public record AuthUser(Long id, String email, String passwordHash, Role role, boolean enabled,
                       Long apartmentId, Long householdId) implements UserDetails {

    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }
}
