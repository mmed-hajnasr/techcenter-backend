package com.isi.techcenter_backend.model;

import com.isi.techcenter_backend.entity.UserRole;

import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull UserRole role) {
}
