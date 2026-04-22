package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualiteUpsertRequest(
                @NotBlank @Size(min = 2, max = 255) String titre,
                @NotBlank @Size(max = 4000) String contenu,
                OffsetDateTime datePublication,
                Boolean estEnAvant) {
}
