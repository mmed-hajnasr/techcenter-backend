package com.isi.techcenter_backend.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank @Size(min = 8, max = 128) String password) {
}
