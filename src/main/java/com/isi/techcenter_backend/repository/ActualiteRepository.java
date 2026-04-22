package com.isi.techcenter_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isi.techcenter_backend.entity.ActualiteEntity;

public interface ActualiteRepository extends JpaRepository<ActualiteEntity, UUID> {
}
