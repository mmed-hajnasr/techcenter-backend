package com.isi.techcenter_backend.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EcritParId implements Serializable {

    @Column(name = "chercheur_id", nullable = false)
    private UUID chercheurId;

    @Column(name = "publication_id", nullable = false)
    private UUID publicationId;

    public UUID getChercheurId() {
        return chercheurId;
    }

    public void setChercheurId(UUID chercheurId) {
        this.chercheurId = chercheurId;
    }

    public UUID getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(UUID publicationId) {
        this.publicationId = publicationId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EcritParId other)) {
            return false;
        }
        return Objects.equals(chercheurId, other.chercheurId)
                && Objects.equals(publicationId, other.publicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chercheurId, publicationId);
    }
}
