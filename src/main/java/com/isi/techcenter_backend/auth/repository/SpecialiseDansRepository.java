package com.isi.techcenter_backend.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isi.techcenter_backend.auth.entity.SpecialiseDansEntity;
import com.isi.techcenter_backend.auth.entity.SpecialiseDansId;

public interface SpecialiseDansRepository extends JpaRepository<SpecialiseDansEntity, SpecialiseDansId> {

    void deleteByChercheur_ChercheurId(UUID chercheurId);
}
