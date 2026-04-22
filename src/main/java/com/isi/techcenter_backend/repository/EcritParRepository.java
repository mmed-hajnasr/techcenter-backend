package com.isi.techcenter_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isi.techcenter_backend.entity.EcritParEntity;
import com.isi.techcenter_backend.entity.EcritParId;

public interface EcritParRepository extends JpaRepository<EcritParEntity, EcritParId> {

    void deleteByPublication_PublicationId(UUID publicationId);
}
