package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.isi.techcenter_backend.entity.UserRole;

public record UserAdminResponse(
        UUID userId,
        String email,
        String username,
        UserRole role,
        OffsetDateTime createdAt) {
}
