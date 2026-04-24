package com.isi.techcenter_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.isi.techcenter_backend.entity.ActualiteEntity;

public interface ActualiteRepository extends JpaRepository<ActualiteEntity, UUID> {

    @Query("""
            select distinct a
            from ActualiteEntity a
            left join fetch a.moderateur
            order by a.estEnAvant desc, a.datePublication desc, a.titre asc
            """)
    List<ActualiteEntity> findAllForUser();
}
