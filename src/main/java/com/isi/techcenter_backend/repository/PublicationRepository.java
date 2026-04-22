package com.isi.techcenter_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.isi.techcenter_backend.entity.PublicationEntity;

public interface PublicationRepository extends JpaRepository<PublicationEntity, UUID> {

    boolean existsByDoiIgnoreCase(String doi);

    boolean existsByDoiIgnoreCaseAndPublicationIdNot(String doi, UUID publicationId);

    @Query("""
            select distinct p
            from PublicationEntity p
            left join fetch p.auteurs a
            left join fetch a.chercheur
            where p.publicationId = :publicationId
            """)
    Optional<PublicationEntity> findByIdWithAuthors(@Param("publicationId") UUID publicationId);

    @Query("""
            select distinct p
            from PublicationEntity p
            left join fetch p.auteurs a
            left join fetch a.chercheur
            order by p.datePublication desc, p.titre asc
            """)
    List<PublicationEntity> findAllForAdmin();
}
