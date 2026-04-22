package com.isi.techcenter_backend.model;

import java.util.UUID;

public record ResearcherSummaryResponse(
        UUID researcherId,
        String name) {
}
