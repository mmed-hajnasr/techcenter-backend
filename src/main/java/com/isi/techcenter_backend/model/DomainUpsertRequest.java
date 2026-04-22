package com.isi.techcenter_backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DomainUpsertRequest(
                @NotBlank @Size(min = 2, max = 255) String name,
                @Size(max = 4000) String description) {
}
