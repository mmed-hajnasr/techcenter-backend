package com.isi.techcenter_backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
        @NotBlank @Size(min = 8, max = 128) String currentPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword) {
}