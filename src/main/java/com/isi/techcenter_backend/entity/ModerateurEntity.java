package com.isi.techcenter_backend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "moderateurs")
@PrimaryKeyJoinColumn(name = "user_id")
public class ModerateurEntity extends UserEntity {

    @OneToMany(mappedBy = "moderateur")
    private List<ActualiteEntity> actualites = new ArrayList<>();

    public List<ActualiteEntity> getActualites() {
        return actualites;
    }

    public void setActualites(List<ActualiteEntity> actualites) {
        this.actualites = actualites;
    }
}
