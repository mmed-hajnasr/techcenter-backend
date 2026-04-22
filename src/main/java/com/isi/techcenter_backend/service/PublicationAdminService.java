package com.isi.techcenter_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.ChercheurEntity;
import com.isi.techcenter_backend.entity.EcritParEntity;
import com.isi.techcenter_backend.entity.EcritParId;
import com.isi.techcenter_backend.entity.PublicationEntity;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.PublicationAdminResponse;
import com.isi.techcenter_backend.model.PublicationUpsertRequest;
import com.isi.techcenter_backend.model.ResearcherSummaryResponse;
import com.isi.techcenter_backend.repository.ChercheurRepository;
import com.isi.techcenter_backend.repository.EcritParRepository;
import com.isi.techcenter_backend.repository.PublicationRepository;

@Service
public class PublicationAdminService {

    private static final String DEFAULT_AUTHOR_ROLE = "AUTHOR";

    private final PublicationRepository publicationRepository;
    private final ChercheurRepository chercheurRepository;
    private final EcritParRepository ecritParRepository;

    public PublicationAdminService(
            PublicationRepository publicationRepository,
            ChercheurRepository chercheurRepository,
            EcritParRepository ecritParRepository) {
        this.publicationRepository = publicationRepository;
        this.chercheurRepository = chercheurRepository;
        this.ecritParRepository = ecritParRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicationAdminResponse> listPublications() {
        return publicationRepository.findAllForAdmin()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PublicationAdminResponse createPublication(PublicationUpsertRequest request) {
        String normalizedTitle = normalizeTitle(request.titre());
        String normalizedDoi = normalizeDoi(request.doi());

        if (normalizedDoi != null && publicationRepository.existsByDoiIgnoreCase(normalizedDoi)) {
            throw new AuthException(AppErrorType.PUBLICATION_DOI_ALREADY_EXISTS, "Publication DOI already exists");
        }

        PublicationEntity publication = new PublicationEntity();
        publication.setTitre(normalizedTitle);
        publication.setResume(normalizeResume(request.resume()));
        publication.setDoi(normalizedDoi);
        publication.setDatePublication(request.datePublication());

        PublicationEntity savedPublication = publicationRepository.save(publication);
        updatePublicationAuthors(savedPublication, request.researcherIds());

        return toResponse(findPublicationWithAuthors(savedPublication.getPublicationId()));
    }

    @Transactional
    public PublicationAdminResponse updatePublication(UUID publicationId, PublicationUpsertRequest request) {
        PublicationEntity publication = findPublicationWithAuthors(publicationId);

        String normalizedTitle = normalizeTitle(request.titre());
        String normalizedDoi = normalizeDoi(request.doi());

        if (normalizedDoi != null
                && publicationRepository.existsByDoiIgnoreCaseAndPublicationIdNot(normalizedDoi, publicationId)) {
            throw new AuthException(AppErrorType.PUBLICATION_DOI_ALREADY_EXISTS, "Publication DOI already exists");
        }

        publication.setTitre(normalizedTitle);
        publication.setResume(normalizeResume(request.resume()));
        publication.setDoi(normalizedDoi);
        publication.setDatePublication(request.datePublication());
        publicationRepository.save(publication);

        updatePublicationAuthors(publication, request.researcherIds());

        return toResponse(findPublicationWithAuthors(publicationId));
    }

    @Transactional
    public void deletePublication(UUID publicationId) {
        PublicationEntity publication = findPublicationWithAuthors(publicationId);
        ecritParRepository.deleteByPublication_PublicationId(publicationId);
        publicationRepository.delete(publication);
    }

    private PublicationEntity findPublicationWithAuthors(UUID publicationId) {
        return publicationRepository.findByIdWithAuthors(publicationId)
                .orElseThrow(() -> new AuthException(AppErrorType.PUBLICATION_NOT_FOUND, "Publication not found"));
    }

    private void updatePublicationAuthors(PublicationEntity publication, List<UUID> researcherIds) {
        Set<UUID> uniqueResearcherIds = new LinkedHashSet<>(researcherIds);
        List<ChercheurEntity> researchers = chercheurRepository.findAllById(uniqueResearcherIds);

        if (researchers.size() != uniqueResearcherIds.size()) {
            throw new AuthException(AppErrorType.RESEARCHER_NOT_FOUND, "One or more researchers were not found");
        }

        ecritParRepository.deleteByPublication_PublicationId(publication.getPublicationId());

        if (researchers.isEmpty()) {
            publication.setAuteurs(new ArrayList<>());
            return;
        }

        List<EcritParEntity> authors = researchers.stream()
                .map(researcher -> {
                    EcritParEntity author = new EcritParEntity();
                    EcritParId id = new EcritParId();
                    id.setPublicationId(publication.getPublicationId());
                    id.setChercheurId(researcher.getChercheurId());
                    author.setId(id);
                    author.setPublication(publication);
                    author.setChercheur(researcher);
                    author.setRole(DEFAULT_AUTHOR_ROLE);
                    return author;
                })
                .toList();

        ecritParRepository.saveAll(authors);
        publication.setAuteurs(new ArrayList<>(authors));
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.length() < 2) {
            throw new AuthException(AppErrorType.INVALID_PUBLICATION_TITLE,
                    "Publication title must be at least 2 characters");
        }
        return normalized;
    }

    private String normalizeResume(String resume) {
        if (resume == null) {
            return null;
        }

        String trimmed = resume.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDoi(String doi) {
        if (doi == null) {
            return null;
        }

        String trimmed = doi.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PublicationAdminResponse toResponse(PublicationEntity publication) {
        List<ResearcherSummaryResponse> authors = publication.getAuteurs()
                .stream()
                .map(EcritParEntity::getChercheur)
                .distinct()
                .map(researcher -> new ResearcherSummaryResponse(researcher.getChercheurId(), researcher.getName()))
                .sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
                .toList();

        return new PublicationAdminResponse(
                publication.getPublicationId(),
                publication.getTitre(),
                publication.getResume(),
                publication.getDoi(),
                publication.getDatePublication(),
                authors);
    }
}
