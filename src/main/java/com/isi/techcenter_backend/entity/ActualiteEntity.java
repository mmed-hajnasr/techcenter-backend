package com.isi.techcenter_backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "actualites")
public class ActualiteEntity {

    @Id
    @UuidGenerator
    @Column(name = "actualite_id", nullable = false, updatable = false)
    private UUID actualiteId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "contenu", nullable = false, length = 4000)
    private String contenu;

    @Column(name = "date_publication")
    private OffsetDateTime datePublication;

    @Column(name = "est_en_avant", nullable = false)
    private Boolean estEnAvant = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moderateur_id", nullable = false)
    private UserEntity moderateur;

    public UUID getActualiteId() {
        return actualiteId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public OffsetDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(OffsetDateTime datePublication) {
        this.datePublication = datePublication;
    }

    public Boolean getEstEnAvant() {
        return estEnAvant;
    }

    public void setEstEnAvant(Boolean estEnAvant) {
        this.estEnAvant = estEnAvant;
    }

    public UserEntity getModerateur() {
        return moderateur;
    }

    public void setModerateur(UserEntity moderateur) {
        this.moderateur = moderateur;
    }
}
