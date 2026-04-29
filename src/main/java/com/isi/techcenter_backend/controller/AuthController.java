package com.isi.techcenter_backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.entity.UserEntity;
import com.isi.techcenter_backend.model.AuthResponse;
import com.isi.techcenter_backend.model.LoginRequest;
import com.isi.techcenter_backend.model.SignupRequest;
import com.isi.techcenter_backend.model.UserPasswordUpdateRequest;
import com.isi.techcenter_backend.model.UserProfileUpdateRequest;
import com.isi.techcenter_backend.service.AuthService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

import jakarta.validation.Valid;

@RestController
@Validated
public class AuthController {

    private final AuthService authService;
    private final EndpointTraceSupport endpointTraceSupport;

    public AuthController(AuthService authService, EndpointTraceSupport endpointTraceSupport) {
        this.authService = authService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return endpointTraceSupport.inSpan(
                "auth.signup",
                "/signup",
                "signup",
                () -> ResponseEntity.ok(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return endpointTraceSupport.inSpan(
                "auth.login",
                "/login",
                "login",
                () -> ResponseEntity.ok(authService.login(request)));
    }

    @GetMapping("/user/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return endpointTraceSupport.inSpan(
                "auth.me",
                "/user/me",
                "get-current-user",
                () -> {
                    UserEntity user = authService.getUserById(userId);
                    return ResponseEntity.ok(new AuthResponse(
                            user.getUserId(),
                            user.getEmail(),
                            user.getUsername(),
                            user.getCreatedAt(),
                            null,
                            0));
                },
                "userId",
                userId.toString());
    }

    @PutMapping("/user/me/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return endpointTraceSupport.inSpan(
                "auth.user.update-profile",
                "/user/me/profile",
                "update-current-user-profile",
                () -> ResponseEntity.ok(authService.updateCurrentUserProfile(userId, request)),
                "userId",
                userId.toString());
    }

    @PutMapping("/user/me/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return endpointTraceSupport.inSpan(
                "auth.user.update-password",
                "/user/me/password",
                "update-current-user-password",
                () -> {
                    authService.updateCurrentUserPassword(userId, request);
                    return ResponseEntity.noContent().build();
                },
                "userId",
                userId.toString());
    }
}
