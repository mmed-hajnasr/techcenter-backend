package com.isi.techcenter_backend.auth.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialise_dans")
public class SpecialiseDansEntity {

    @EmbeddedId
    private SpecialiseDansId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("chercheurId")
    @JoinColumn(name = "chercheur_id", nullable = false)
    private ChercheurEntity chercheur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("domaineId")
    @JoinColumn(name = "domaine_id", nullable = false)
    private DomaineEntity domaine;

    @Column(name = "date_association")
    private OffsetDateTime dateAssociation;

    public SpecialiseDansId getId() {
        return id;
    }

    public void setId(SpecialiseDansId id) {
        this.id = id;
    }

    public ChercheurEntity getChercheur() {
        return chercheur;
    }

    public void setChercheur(ChercheurEntity chercheur) {
        this.chercheur = chercheur;
    }

    public DomaineEntity getDomaine() {
        return domaine;
    }

    public void setDomaine(DomaineEntity domaine) {
        this.domaine = domaine;
    }

    public OffsetDateTime getDateAssociation() {
        return dateAssociation;
    }

    public void setDateAssociation(OffsetDateTime dateAssociation) {
        this.dateAssociation = dateAssociation;
    }
}
