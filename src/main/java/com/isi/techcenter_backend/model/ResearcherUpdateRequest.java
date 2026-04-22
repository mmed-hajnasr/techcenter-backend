package com.isi.techcenter_backend.model;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResearcherUpdateRequest(
                @NotBlank @Size(min = 3, max = 255) String name,
                @Size(max = 4000) String biographie,
                List<UUID> domainIds) {
}
