package com.isi.techcenter_backend.auth.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "publications")
public class PublicationEntity {

    @Id
    @UuidGenerator
    @Column(name = "publication_id", nullable = false, updatable = false)
    private UUID publicationId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "resume", length = 4000)
    private String resume;

    @Column(name = "doi", unique = true)
    private String doi;

    @Lob
    @Column(name = "fichier_pdf")
    private byte[] fichierPdf;

    @Column(name = "date_publication")
    private OffsetDateTime datePublication;

    @OneToMany(mappedBy = "publication")
    private List<EcritParEntity> auteurs = new ArrayList<>();

    public UUID getPublicationId() {
        return publicationId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public byte[] getFichierPdf() {
        return fichierPdf;
    }

    public void setFichierPdf(byte[] fichierPdf) {
        this.fichierPdf = fichierPdf;
    }

    public OffsetDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(OffsetDateTime datePublication) {
        this.datePublication = datePublication;
    }

    public List<EcritParEntity> getAuteurs() {
        return auteurs;
    }

    public void setAuteurs(List<EcritParEntity> auteurs) {
        this.auteurs = auteurs;
    }
}
