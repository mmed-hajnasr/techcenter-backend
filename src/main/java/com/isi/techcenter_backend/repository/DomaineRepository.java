package com.isi.techcenter_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isi.techcenter_backend.entity.DomaineEntity;

public interface DomaineRepository extends JpaRepository<DomaineEntity, UUID> {

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndDomaineIdNot(String nom, UUID domaineId);
}
