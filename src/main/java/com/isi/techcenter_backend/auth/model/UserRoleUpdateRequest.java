package com.isi.techcenter_backend.auth.model;

import com.isi.techcenter_backend.auth.entity.UserRole;

import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull UserRole role) {
}
