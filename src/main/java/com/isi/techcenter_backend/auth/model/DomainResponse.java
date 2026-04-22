package com.isi.techcenter_backend.auth.model;

import java.util.UUID;

public record DomainResponse(
                UUID domainId,
                String name,
                String description) {
}
