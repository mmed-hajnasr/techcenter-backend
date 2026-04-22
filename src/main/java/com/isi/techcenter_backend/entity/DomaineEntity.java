package com.isi.techcenter_backend.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "domaines")
public class DomaineEntity {

    @Id
    @UuidGenerator
    @Column(name = "domaine_id", nullable = false, updatable = false)
    private UUID domaineId;

    @Column(name = "nom", nullable = false, unique = true)
    private String nom;

    @Column(name = "description", length = 4000)
    private String description;

    @OneToMany(mappedBy = "domaine")
    private List<SpecialiseDansEntity> specialisations = new ArrayList<>();

    public UUID getDomaineId() {
        return domaineId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SpecialiseDansEntity> getSpecialisations() {
        return specialisations;
    }

    public void setSpecialisations(List<SpecialiseDansEntity> specialisations) {
        this.specialisations = specialisations;
    }
}
