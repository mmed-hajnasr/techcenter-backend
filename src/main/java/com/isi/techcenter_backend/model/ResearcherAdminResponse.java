package com.isi.techcenter_backend.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResearcherAdminResponse(
                UUID researcherId,
                String name,
                String biographie,
                List<DomainResponse> domains,
                OffsetDateTime createdAt) {
}
