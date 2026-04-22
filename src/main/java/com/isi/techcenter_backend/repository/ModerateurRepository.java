package com.isi.techcenter_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isi.techcenter_backend.entity.ModerateurEntity;

public interface ModerateurRepository extends JpaRepository<ModerateurEntity, UUID> {
}
