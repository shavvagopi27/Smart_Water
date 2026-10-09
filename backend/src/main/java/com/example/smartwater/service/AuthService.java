package com.example.smartwater.service;

import com.example.smartwater.dto.AuthDtos.*;
import com.example.smartwater.dto.UserDtos.UserResponse;
import com.example.smartwater.entity.Role;
import com.example.smartwater.entity.User;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.repository.UserRepository;
import com.example.smartwater.security.JwtService;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Public self-registration always creates a RESIDENT. An admin then assigns the household. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone());
        user.setRole(Role.RESIDENT);
        user = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), Mappers.toUser(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(User::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return new AuthResponse(jwtService.generateToken(user), Mappers.toUser(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        User user = userRepository.findById(SecurityUtils.currentUser().id()).orElseThrow();
        return Mappers.toUser(user);
    }
}
