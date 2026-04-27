package com.isi.techcenter_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final MinioStorageService minioStorageService;
    private final Tracer tracer;

    public PublicationAdminService(
            PublicationRepository publicationRepository,
            ChercheurRepository chercheurRepository,
            EcritParRepository ecritParRepository,
            MinioStorageService minioStorageService,
            Tracer tracer) {
        this.publicationRepository = publicationRepository;
        this.chercheurRepository = chercheurRepository;
        this.ecritParRepository = ecritParRepository;
        this.minioStorageService = minioStorageService;
        this.tracer = tracer;
    }

    @Transactional(readOnly = true)
    public List<PublicationAdminResponse> listPublications() {
        return inStep(
                "admin.publications.db-list",
                () -> publicationRepository.findAllForAdmin()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                "step",
                "db-list-publications");
    }

    @Transactional
    public PublicationAdminResponse createPublication(PublicationUpsertRequest request) {
        String normalizedTitle = normalizeTitle(request.titre());
        String normalizedDoi = normalizeDoi(request.doi());

        boolean doiExists = normalizedDoi != null && inStep(
                "admin.publications.db-check-doi-exists",
                () -> publicationRepository.existsByDoiIgnoreCase(normalizedDoi),
                "step",
                "db-check-doi-exists",
                "doi",
                normalizedDoi);
        if (doiExists) {
            throw new AuthException(AppErrorType.PUBLICATION_DOI_ALREADY_EXISTS, "Publication DOI already exists");
        }

        PublicationEntity publication = new PublicationEntity();
        publication.setTitre(normalizedTitle);
        publication.setResume(normalizeResume(request.resume()));
        publication.setDoi(normalizedDoi);
        publication.setDatePublication(request.datePublication());

        PublicationEntity savedPublication = inStep(
                "admin.publications.db-save",
                () -> publicationRepository.save(publication),
                "step",
                "db-save-publication",
                "title",
                normalizedTitle);
        updatePublicationAuthors(savedPublication, request.researcherIds());

        return toResponse(findPublicationWithAuthors(savedPublication.getPublicationId()));
    }

    @Transactional
    public PublicationAdminResponse updatePublication(UUID publicationId, PublicationUpsertRequest request) {
        PublicationEntity publication = findPublicationWithAuthors(publicationId);

        String normalizedTitle = normalizeTitle(request.titre());
        String normalizedDoi = normalizeDoi(request.doi());

        boolean doiExists = normalizedDoi != null
                && inStep(
                        "admin.publications.db-check-doi-exists-for-update",
                        () -> publicationRepository.existsByDoiIgnoreCaseAndPublicationIdNot(normalizedDoi,
                                publicationId),
                        "step",
                        "db-check-doi-exists-for-update",
                        "publicationId",
                        publicationId.toString(),
                        "doi",
                        normalizedDoi);
        if (doiExists) {
            throw new AuthException(AppErrorType.PUBLICATION_DOI_ALREADY_EXISTS, "Publication DOI already exists");
        }

        publication.setTitre(normalizedTitle);
        publication.setResume(normalizeResume(request.resume()));
        publication.setDoi(normalizedDoi);
        publication.setDatePublication(request.datePublication());
        inStep(
                "admin.publications.db-save-update",
                () -> publicationRepository.save(publication),
                "step",
                "db-save-update-publication",
                "publicationId",
                publicationId.toString(),
                "title",
                normalizedTitle);

        updatePublicationAuthors(publication, request.researcherIds());

        return toResponse(findPublicationWithAuthors(publicationId));
    }

    @Transactional
    public void deletePublication(UUID publicationId) {
        PublicationEntity publication = findPublicationWithAuthors(publicationId);
        if (publication.getFichierPdfPath() != null) {
            inStep(
                    "admin.publications.minio-delete-pdf",
                    () -> {
                        minioStorageService.deletePublicationPdf(publication.getFichierPdfPath());
                        return null;
                    },
                    "step",
                    "delete-publication-pdf",
                    "publicationId",
                    publicationId.toString());
        }
        inStep(
                "admin.publications.db-delete-authors",
                () -> {
                    ecritParRepository.deleteByPublication_PublicationId(publicationId);
                    return null;
                },
                "step",
                "db-delete-publication-authors",
                "publicationId",
                publicationId.toString());
        inStep(
                "admin.publications.db-delete",
                () -> {
                    publicationRepository.delete(publication);
                    return null;
                },
                "step",
                "db-delete-publication",
                "publicationId",
                publicationId.toString());
    }

    @Transactional
    public PublicationAdminResponse uploadPublicationPdf(UUID publicationId, MultipartFile pdf) {
        PublicationEntity publication = findPublicationWithAuthors(publicationId);
        String pdfPath = inStep(
                "admin.publications.minio-store-pdf",
                () -> minioStorageService.storePublicationPdf(publicationId, pdf),
                "step",
                "store-publication-pdf",
                "publicationId",
                publicationId.toString());
        publication.setFichierPdfPath(pdfPath);
        inStep(
                "admin.publications.db-save-pdf-path",
                () -> publicationRepository.save(publication),
                "step",
                "save-publication-pdf-path",
                "publicationId",
                publicationId.toString());
        return toResponse(findPublicationWithAuthors(publicationId));
    }

    @Transactional
    public PublicationAdminResponse deletePublicationPdf(UUID publicationId) {
        PublicationEntity publication = findPublicationWithAuthors(publicationId);
        if (publication.getFichierPdfPath() != null) {
            inStep(
                    "admin.publications.minio-delete-pdf",
                    () -> {
                        minioStorageService.deletePublicationPdf(publication.getFichierPdfPath());
                        return null;
                    },
                    "step",
                    "delete-publication-pdf",
                    "publicationId",
                    publicationId.toString());
        }
        publication.setFichierPdfPath(null);
        inStep(
                "admin.publications.db-clear-pdf-path",
                () -> publicationRepository.save(publication),
                "step",
                "clear-publication-pdf-path",
                "publicationId",
                publicationId.toString());
        return toResponse(findPublicationWithAuthors(publicationId));
    }

    private PublicationEntity findPublicationWithAuthors(UUID publicationId) {
        return inStep(
                "admin.publications.db-find-by-id-with-authors",
                () -> publicationRepository.findByIdWithAuthors(publicationId)
                        .orElseThrow(
                                () -> new AuthException(AppErrorType.PUBLICATION_NOT_FOUND, "Publication not found")),
                "step",
                "db-find-publication-by-id-with-authors",
                "publicationId",
                publicationId.toString());
    }

    private void updatePublicationAuthors(PublicationEntity publication, List<UUID> researcherIds) {
        Set<UUID> uniqueResearcherIds = new LinkedHashSet<>(researcherIds);
        List<ChercheurEntity> researchers = inStep(
                "admin.publications.db-find-researchers-by-ids",
                () -> chercheurRepository.findAllById(uniqueResearcherIds),
                "step",
                "db-find-researchers-by-ids",
                "publicationId",
                publication.getPublicationId().toString(),
                "researcherCount",
                Integer.toString(uniqueResearcherIds.size()));

        if (researchers.size() != uniqueResearcherIds.size()) {
            throw new AuthException(AppErrorType.RESEARCHER_NOT_FOUND, "One or more researchers were not found");
        }

        inStep(
                "admin.publications.db-delete-authors",
                () -> {
                    ecritParRepository.deleteByPublication_PublicationId(publication.getPublicationId());
                    return null;
                },
                "step",
                "db-delete-publication-authors",
                "publicationId",
                publication.getPublicationId().toString());

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

        inStep(
                "admin.publications.db-save-authors",
                () -> ecritParRepository.saveAll(authors),
                "step",
                "db-save-publication-authors",
                "publicationId",
                publication.getPublicationId().toString(),
                "authorCount",
                Integer.toString(authors.size()));
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
                authors,
            minioStorageService.getPublicationPdfPresignedUrl(publication.getFichierPdfPath()));
    }

    private <T> T inStep(String stepSpanName, Supplier<T> action, String... tagPairs) {
        Span span = tracer.nextSpan().name(stepSpanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            applyTags(span, tagPairs);
            return action.get();
        } finally {
            span.end();
        }
    }

    private void applyTags(Span span, String... tagPairs) {
        if (tagPairs == null || tagPairs.length == 0) {
            return;
        }
        for (int index = 0; index + 1 < tagPairs.length; index += 2) {
            String key = tagPairs[index];
            String value = tagPairs[index + 1];
            if (key == null || value == null) {
                continue;
            }
            span.tag(key, value);
        }
    }
}
