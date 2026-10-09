package com.example.smartwater.service;

import com.example.smartwater.dto.UserDtos.*;
import com.example.smartwater.entity.Household;
import com.example.smartwater.entity.Role;
import com.example.smartwater.entity.User;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.UserRepository;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final HouseholdService householdService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Role role) {
        List<User> users = role == null ? userRepository.findAll() : userRepository.findByRoleOrderByIdAsc(role);
        return users.stream().map(Mappers::toUser).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        if (!me.isAdmin() && !me.id().equals(id)) {
            throw new AccessDeniedException("You can only view your own profile");
        }
        return Mappers.toUser(get(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        AuthUser me = SecurityUtils.currentUser();
        if (!me.isAdmin() && !me.id().equals(id)) {
            throw new AccessDeniedException("You can only update your own profile");
        }
        User user = get(id);
        user.setName(req.name().trim());
        user.setPhone(req.phone());
        if (me.isAdmin()) { // only admins may re-assign households or disable accounts
            if (req.householdId() != null) {
                Household h = householdService.get(req.householdId());
                user.setHousehold(h);
                user.setApartment(h.getApartment());
            }
            if (req.enabled() != null) {
                if (!req.enabled() && me.id().equals(id)) {
                    throw new BadRequestException("You cannot disable your own account");
                }
                user.setEnabled(req.enabled());
            }
        }
        return Mappers.toUser(userRepository.save(user));
    }

    @Transactional
    public UserResponse addResident(Long householdId, ResidentRequest req) {
        Household h = householdService.get(householdId);
        String email = req.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            if (req.password() == null || req.password().isBlank()) {
                throw new BadRequestException("A password (min 8 characters) is required to create a new resident account");
            }
            user = new User();
            user.setName(req.name().trim());
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(req.password()));
            user.setPhone(req.phone());
            user.setRole(Role.RESIDENT);
        } else if (user.getRole() != Role.RESIDENT) {
            throw new ConflictException("This email belongs to an admin account");
        } else if (user.getHousehold() != null && !user.getHousehold().getId().equals(householdId)) {
            throw new ConflictException("This resident is already assigned to another household");
        }
        user.setHousehold(h);
        user.setApartment(h.getApartment());
        return Mappers.toUser(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listResidents(Long householdId) {
        SecurityUtils.requireHouseholdAccess(householdId);
        householdService.get(householdId);
        return userRepository.findByHouseholdIdOrderByIdAsc(householdId).stream().map(Mappers::toUser).toList();
    }

    @Transactional
    public void removeResident(Long householdId, Long userId) {
        User user = get(userId);
        if (user.getHousehold() == null || !user.getHousehold().getId().equals(householdId)) {
            throw new ResourceNotFoundException("Resident not found in this household");
        }
        user.setHousehold(null);
        userRepository.save(user);
    }

    private User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
