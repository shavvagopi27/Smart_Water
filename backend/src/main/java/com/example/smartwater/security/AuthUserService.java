package com.example.smartwater.security;

import com.example.smartwater.entity.User;
import com.example.smartwater.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<AuthUser> loadById(Long id) {
        return userRepository.findById(id).filter(User::isEnabled).map(u -> new AuthUser(
                u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole(), u.isEnabled(),
                u.getApartment() == null ? null : u.getApartment().getId(),
                u.getHousehold() == null ? null : u.getHousehold().getId()));
    }
}
