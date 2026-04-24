package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActualiteUserResponse(
        UUID actualiteId,
        String titre,
        String contenu,
        OffsetDateTime datePublication,
        Boolean estEnAvant,
        UUID moderateurId) {
}
