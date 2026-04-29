package com.isi.techcenter_backend.service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.UserEntity;
import com.isi.techcenter_backend.entity.UserRole;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.AuthResponse;
import com.isi.techcenter_backend.model.LoginRequest;
import com.isi.techcenter_backend.model.SignupRequest;
import com.isi.techcenter_backend.model.UserPasswordUpdateRequest;
import com.isi.techcenter_backend.model.UserProfileUpdateRequest;
import com.isi.techcenter_backend.repository.UserRepository;
import com.isi.techcenter_backend.security.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final Tracer tracer;

    public AuthService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtService jwtService,
            Tracer tracer) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.tracer = tracer;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String normalizedEmail = inStep("auth.signup.normalize-email", () -> request.email().trim().toLowerCase(),
                "step", "normalize-email");
        String normalizedUsername = inStep("auth.signup.normalize-username", () -> request.username().trim(),
                "step", "normalize-username");

        if (normalizedUsername.length() < 3) {
            throw new AuthException(AppErrorType.INVALID_USERNAME, "Username must be at least 3 characters");
        }

        boolean emailExists = inStep(
                "auth.signup.db-check-email-exists",
                () -> userRepository.existsByEmailIgnoreCase(normalizedEmail),
                "step",
                "check-email-exists");

        if (emailExists) {
            throw new AuthException(AppErrorType.INVALID_EMAIL, "Email is already in use");
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        String passwordHash = inStep(
                "auth.signup.hash-password",
                () -> passwordService.hashPassword(request.password()),
                "step",
                "hash-password");
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.USER);

        UserEntity savedUser = inStep(
                "auth.signup.db-save-user",
                () -> userRepository.save(user),
                "step",
                "save-user");
        String token = inStep(
                "auth.signup.generate-token",
                () -> jwtService.generateAccessToken(savedUser.getUserId(), savedUser.getRole()),
                "step",
                "generate-token");

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
        String normalizedIdentifier = inStep(
                "auth.login.normalize-identifier",
                () -> request.identifier().trim(),
                "step",
                "normalize-identifier");

        Optional<UserEntity> user = inStep(
                "auth.login.db-lookup-user",
                () -> userRepository.findByEmailIgnoreCase(normalizedIdentifier)
                        .or(() -> userRepository.findFirstByUsernameIgnoreCase(normalizedIdentifier)),
                "step",
                "lookup-user");

        if (user.isEmpty()) {
            inStep("auth.login.verify-fallback", () -> {
                passwordService.verifyAgainstFallback(request.password());
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null) {
                    currentSpan.tag("login.result", "user-not-found");
                    currentSpan.event("auth.login.incorrect_credentials.user-not-found");
                }
                log.info("auth.login.incorrect_credentials reason=user-not-found");
                return null;
            }, "step", "verify-fallback");
            throw new AuthException(AppErrorType.INCORRECT_LOGIN, "Incorrect credentials");
        }

        UserEntity existingUser = user.get();
        boolean passwordMatches = inStep(
                "auth.login.verify-password",
                () -> {
                    boolean verified = passwordService.verifyPassword(request.password(),
                            existingUser.getPasswordHash());
                    String verificationResult = verified ? "match" : "mismatch";
                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        currentSpan.tag("password.verification.result", verificationResult);
                        currentSpan.event("auth.login.password_verification." + verificationResult);
                    }
                    log.info(
                            "auth.login.password_verification result={} userId={}",
                            verificationResult,
                            existingUser.getUserId());
                    return verified;
                },
                "step",
                "verify-password",
                "userId",
                existingUser.getUserId().toString());

        if (!passwordMatches) {
            throw new AuthException(AppErrorType.INCORRECT_LOGIN, "Incorrect credentials");
        }

        String token = inStep(
                "auth.login.generate-token",
                () -> jwtService.generateAccessToken(existingUser.getUserId(), existingUser.getRole()),
                "step",
                "generate-token",
                "userId",
                existingUser.getUserId().toString());

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
        return inStep(
                "auth.user.db-find-by-id",
                () -> userRepository.findById(userId)
                        .orElseThrow(() -> new AuthException(AppErrorType.NOT_LOGGED_IN, "User not found")),
                "step",
                "find-user-by-id",
                "userId",
                userId.toString());
    }

    @Transactional
    public AuthResponse updateCurrentUserProfile(UUID userId, UserProfileUpdateRequest request) {
        UserEntity user = getUserById(userId);

        String normalizedEmail = inStep(
                "auth.user.update-profile.normalize-email",
                () -> request.email().trim().toLowerCase(),
                "step",
                "normalize-email",
                "userId",
                userId.toString());
        String normalizedUsername = inStep(
                "auth.user.update-profile.normalize-username",
                () -> request.username().trim(),
                "step",
                "normalize-username",
                "userId",
                userId.toString());

        if (normalizedUsername.length() < 3) {
            throw new AuthException(AppErrorType.INVALID_USERNAME, "Username must be at least 3 characters");
        }

        boolean emailExists = inStep(
                "auth.user.update-profile.db-check-email-exists",
                () -> userRepository.existsByEmailIgnoreCaseAndUserIdNot(normalizedEmail, userId),
                "step",
                "check-email-exists",
                "userId",
                userId.toString());
        if (emailExists) {
            throw new AuthException(AppErrorType.INVALID_EMAIL, "Email is already in use");
        }

        boolean usernameExists = inStep(
                "auth.user.update-profile.db-check-username-exists",
                () -> userRepository.existsByUsernameIgnoreCaseAndUserIdNot(normalizedUsername, userId),
                "step",
                "check-username-exists",
                "userId",
                userId.toString());
        if (usernameExists) {
            throw new AuthException(AppErrorType.INVALID_USERNAME, "Username is already in use");
        }

        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);

        UserEntity savedUser = inStep(
                "auth.user.update-profile.db-save",
                () -> userRepository.save(user),
                "step",
                "save-user",
                "userId",
                userId.toString());

        return new AuthResponse(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                savedUser.getCreatedAt(),
                null,
                0);
    }

    @Transactional
    public void updateCurrentUserPassword(UUID userId, UserPasswordUpdateRequest request) {
        UserEntity user = getUserById(userId);

        boolean currentPasswordMatches = inStep(
                "auth.user.update-password.verify-current",
                () -> passwordService.verifyPassword(request.currentPassword(), user.getPasswordHash()),
                "step",
                "verify-current-password",
                "userId",
                userId.toString());
        if (!currentPasswordMatches) {
            throw new AuthException(AppErrorType.INVALID_PASSWORD, "Current password is incorrect");
        }

        boolean reusingSamePassword = inStep(
                "auth.user.update-password.verify-new",
                () -> passwordService.verifyPassword(request.newPassword(), user.getPasswordHash()),
                "step",
                "verify-new-password-different",
                "userId",
                userId.toString());
        if (reusingSamePassword) {
            throw new AuthException(AppErrorType.INVALID_PASSWORD,
                    "New password must be different from current password");
        }

        String newPasswordHash = inStep(
                "auth.user.update-password.hash-new",
                () -> passwordService.hashPassword(request.newPassword()),
                "step",
                "hash-new-password",
                "userId",
                userId.toString());

        user.setPasswordHash(newPasswordHash);
        inStep(
                "auth.user.update-password.db-save",
                () -> userRepository.save(user),
                "step",
                "save-user-password",
                "userId",
                userId.toString());
    }

    private <T> T inStep(String stepSpanName, Supplier<T> action, String... tagPairs) {
        Span span = tracer.nextSpan().name(stepSpanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            applyTags(span, tagPairs);
            return action.get();
        } finally {
            span.end();
        }
    }

    private void applyTags(Span span, String... tagPairs) {
        if (tagPairs == null || tagPairs.length == 0) {
            return;
        }
        for (int index = 0; index + 1 < tagPairs.length; index += 2) {
            String key = tagPairs[index];
            String value = tagPairs[index + 1];
            if (key == null || value == null) {
                continue;
            }
            span.tag(key, value);
        }
    }
}
