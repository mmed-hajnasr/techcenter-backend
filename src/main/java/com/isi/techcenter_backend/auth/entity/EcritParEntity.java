package com.isi.techcenter_backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "ecrit_par")
public class EcritParEntity {

    @EmbeddedId
    private EcritParId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("chercheurId")
    @JoinColumn(name = "chercheur_id", nullable = false)
    private ChercheurEntity chercheur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("publicationId")
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationEntity publication;

    @Column(name = "role", nullable = false)
    private String role;

    public EcritParId getId() {
        return id;
    }

    public void setId(EcritParId id) {
        this.id = id;
    }

    public ChercheurEntity getChercheur() {
        return chercheur;
    }

    public void setChercheur(ChercheurEntity chercheur) {
        this.chercheur = chercheur;
    }

    public PublicationEntity getPublication() {
        return publication;
    }

    public void setPublication(PublicationEntity publication) {
        this.publication = publication;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
