package com.isi.techcenter_backend.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.UserEntity;
import com.isi.techcenter_backend.entity.UserRole;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.AuthResponse;
import com.isi.techcenter_backend.model.LoginRequest;
import com.isi.techcenter_backend.model.SignupRequest;
import com.isi.techcenter_backend.repository.UserRepository;
import com.isi.techcenter_backend.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordService passwordService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedUsername = request.username().trim();

        if (normalizedUsername.length() < 3) {
            throw new AuthException(AppErrorType.INVALID_USERNAME, "Username must be at least 3 characters");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AuthException(AppErrorType.INVALID_EMAIL, "Email is already in use");
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordService.hashPassword(request.password()));
        user.setRole(UserRole.USER);

        UserEntity savedUser = userRepository.save(user);
        String token = jwtService.generateAccessToken(savedUser.getUserId(), savedUser.getRole());

        return new AuthResponse(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                savedUser.getCreatedAt(),
                token,
                jwtService.getExpirationSeconds());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedIdentifier = request.identifier().trim();

        Optional<UserEntity> user = userRepository.findByEmailIgnoreCase(normalizedIdentifier)
                .or(() -> userRepository.findFirstByUsernameIgnoreCase(normalizedIdentifier));

        if (user.isEmpty()) {
            passwordService.verifyAgainstFallback(request.password());
            throw new AuthException(AppErrorType.INCORRECT_LOGIN, "Incorrect credentials");
        }

        UserEntity existingUser = user.get();
        if (!passwordService.verifyPassword(request.password(), existingUser.getPasswordHash())) {
            throw new AuthException(AppErrorType.INCORRECT_LOGIN, "Incorrect credentials");
        }

        String token = jwtService.generateAccessToken(existingUser.getUserId(), existingUser.getRole());

        return new AuthResponse(
                existingUser.getUserId(),
                existingUser.getEmail(),
                existingUser.getUsername(),
                existingUser.getCreatedAt(),
                token,
                jwtService.getExpirationSeconds());
    }

    @Transactional(readOnly = true)
    public UserEntity getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AppErrorType.NOT_LOGGED_IN, "User not found"));
    }
}
