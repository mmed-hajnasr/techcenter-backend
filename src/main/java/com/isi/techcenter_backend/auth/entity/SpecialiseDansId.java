package com.isi.techcenter_backend.auth.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SpecialiseDansId implements Serializable {

    @Column(name = "chercheur_id", nullable = false)
    private UUID chercheurId;

    @Column(name = "domaine_id", nullable = false)
    private UUID domaineId;

    public UUID getChercheurId() {
        return chercheurId;
    }

    public void setChercheurId(UUID chercheurId) {
        this.chercheurId = chercheurId;
    }

    public UUID getDomaineId() {
        return domaineId;
    }

    public void setDomaineId(UUID domaineId) {
        this.domaineId = domaineId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpecialiseDansId other)) {
            return false;
        }
        return Objects.equals(chercheurId, other.chercheurId)
                && Objects.equals(domaineId, other.domaineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chercheurId, domaineId);
    }
}
