package com.isi.techcenter_backend.auth.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "chercheurs")
public class ChercheurEntity {

    @Id
    @UuidGenerator
    @Column(name = "chercheur_id", nullable = false, updatable = false)
    private UUID chercheurId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "biographie", length = 4000)
    private String biographie;

    @Lob
    @Column(name = "photo")
    private byte[] photo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "chercheur")
    private List<EcritParEntity> publicationsEcrites = new ArrayList<>();

    @OneToMany(mappedBy = "chercheur")
    private List<SpecialiseDansEntity> specialisations = new ArrayList<>();

    public UUID getChercheurId() {
        return chercheurId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBiographie() {
        return biographie;
    }

    public void setBiographie(String biographie) {
        this.biographie = biographie;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<EcritParEntity> getPublicationsEcrites() {
        return publicationsEcrites;
    }

    public void setPublicationsEcrites(List<EcritParEntity> publicationsEcrites) {
        this.publicationsEcrites = publicationsEcrites;
    }

    public List<SpecialiseDansEntity> getSpecialisations() {
        return specialisations;
    }

    public void setSpecialisations(List<SpecialiseDansEntity> specialisations) {
        this.specialisations = specialisations;
    }
}
