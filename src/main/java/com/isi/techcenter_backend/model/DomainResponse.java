package com.isi.techcenter_backend.model;

import java.util.UUID;

public record DomainResponse(
                UUID domainId,
                String name,
                String description) {
}
