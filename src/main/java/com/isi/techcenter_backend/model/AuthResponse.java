package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String username,
        OffsetDateTime createdAt,
        String accessToken,
        long expiresInSeconds) {
}
