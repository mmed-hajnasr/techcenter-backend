package com.isi.techcenter_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.isi.techcenter_backend.entity.ChercheurEntity;

public interface ChercheurRepository extends JpaRepository<ChercheurEntity, UUID> {

        @Query("""
                        select distinct c
                        from ChercheurEntity c
                        left join fetch c.specialisations s
                        left join fetch s.domaine d
                        where lower(c.name) like concat('%', :name, '%')
                        order by c.name asc
                        """)
        List<ChercheurEntity> searchForAdminByName(@Param("name") String name);

        @Query("""
                        select distinct c
                        from ChercheurEntity c
                        left join fetch c.specialisations s
                        left join fetch s.domaine
                        where c.chercheurId = :chercheurId
                        """)
        Optional<ChercheurEntity> findByIdWithSpecialisations(@Param("chercheurId") UUID chercheurId);

        boolean existsByEmailIgnoreCase(String email);

        boolean existsByEmailIgnoreCaseAndChercheurIdNot(String email, UUID chercheurId);
}
