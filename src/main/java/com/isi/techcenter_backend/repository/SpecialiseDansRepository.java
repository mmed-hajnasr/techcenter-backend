package com.isi.techcenter_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isi.techcenter_backend.entity.SpecialiseDansEntity;
import com.isi.techcenter_backend.entity.SpecialiseDansId;

public interface SpecialiseDansRepository extends JpaRepository<SpecialiseDansEntity, SpecialiseDansId> {

    void deleteByChercheur_ChercheurId(UUID chercheurId);
}
