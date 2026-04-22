package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PublicationAdminResponse(
        UUID publicationId,
        String titre,
        String resume,
        String doi,
        OffsetDateTime datePublication,
        List<ResearcherSummaryResponse> authors) {
}
