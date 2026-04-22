package com.isi.techcenter_backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final String fallbackHash;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.fallbackHash = passwordEncoder.encode("fallback-password-not-used");
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void verifyAgainstFallback(String rawPassword) {
        passwordEncoder.matches(rawPassword, fallbackHash);
    }
}
