package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicationUpsertRequest(
        @NotBlank @Size(min = 2, max = 255) String titre,
        @Size(max = 4000) String resume,
        @Size(max = 255) String doi,
        OffsetDateTime datePublication,
        @NotNull List<UUID> researcherIds) {
}
