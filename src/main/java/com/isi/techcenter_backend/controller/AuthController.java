package com.isi.techcenter_backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.entity.UserEntity;
import com.isi.techcenter_backend.model.AuthResponse;
import com.isi.techcenter_backend.model.LoginRequest;
import com.isi.techcenter_backend.model.SignupRequest;
import com.isi.techcenter_backend.service.AuthService;

import jakarta.validation.Valid;

@RestController
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/user/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserEntity user = authService.getUserById(userId);

        return ResponseEntity.ok(new AuthResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getCreatedAt(),
                null,
                0));
    }
}
