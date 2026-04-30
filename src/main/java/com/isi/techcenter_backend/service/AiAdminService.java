package com.isi.techcenter_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.isi.techcenter_backend.config.AiServerProperties;
import com.isi.techcenter_backend.entity.ChercheurEntity;
import com.isi.techcenter_backend.entity.PublicationEntity;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.AiBiographyResponse;
import com.isi.techcenter_backend.model.AiNewsArticleResponse;
import com.isi.techcenter_backend.model.AiPaperResponse;
import com.isi.techcenter_backend.repository.ChercheurRepository;
import com.isi.techcenter_backend.repository.PublicationRepository;

@Service
public class AiAdminService {

    private final ChercheurRepository chercheurRepository;
    private final PublicationRepository publicationRepository;
    private final MinioStorageService minioStorageService;
    private final RestTemplate restTemplate;
    private final String aiServerUrl;

    public AiAdminService(
            ChercheurRepository chercheurRepository,
            PublicationRepository publicationRepository,
            MinioStorageService minioStorageService,
            RestTemplate restTemplate,
            AiServerProperties aiServerProperties) {
        this.chercheurRepository = chercheurRepository;
        this.publicationRepository = publicationRepository;
        this.minioStorageService = minioStorageService;
        this.restTemplate = restTemplate;
        this.aiServerUrl = aiServerProperties.serverUrl();
    }

    // ── Internal request/response records ─────────────────────────────────────

    record PublicationInput(String title, String resume) {
    }

    record BiographyRequest(
            @JsonProperty("researcher_name") String researcherName,
            List<PublicationInput> publications) {
    }

    record BiographyPayload(String name, String biography,
            @JsonProperty("research_areas") List<String> researchAreas) {
    }

    record BiographyApiResponse(String type, BiographyPayload payload, String message) {
    }

    record PaperRequest(@JsonProperty("pdf_url") String pdfUrl) {
    }

    record PaperPayload(String title, String resume) {
    }

    record PaperApiResponse(String type, PaperPayload payload, String message) {
    }

    record ResearcherInput(String name, String biography) {
    }

    record NewsArticleRequest(String title, String resume, List<ResearcherInput> researchers) {
    }

    record NewsArticlePayload(String headline, String article) {
    }

    record NewsArticleApiResponse(String type, NewsArticlePayload payload, String message) {
    }

    // ── Public service methods ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AiBiographyResponse generateBiography(UUID researcherId) {
        ChercheurEntity researcher = chercheurRepository.findByIdWithPublications(researcherId)
                .orElseThrow(() -> new AuthException(AppErrorType.RESEARCHER_NOT_FOUND,
                        "Researcher not found: " + researcherId));

        List<PublicationInput> publications = researcher.getPublicationsEcrites().stream()
                .map(ep -> ep.getPublication())
                .map(p -> new PublicationInput(p.getTitre(), p.getResume()))
                .toList();

        BiographyRequest request = new BiographyRequest(researcher.getName(), publications);
        BiographyApiResponse response = restTemplate.postForObject(
                aiServerUrl + "/biography", request, BiographyApiResponse.class);

        if (response == null || !"biography".equals(response.type())) {
            String msg = response != null ? response.message() : "No response from AI server";
            throw new AuthException(AppErrorType.INVALID_PUBLICATION_TITLE,
                    "AI biography generation failed: " + msg);
        }

        BiographyPayload payload = response.payload();
        return new AiBiographyResponse(payload.name(), payload.biography(), payload.researchAreas());
    }

    @Transactional(readOnly = true)
    public AiPaperResponse generatePaper(UUID publicationId) {
        PublicationEntity publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new AuthException(AppErrorType.PUBLICATION_NOT_FOUND,
                        "Publication not found: " + publicationId));

        String pdfUrl = minioStorageService.getPublicationPdfPresignedUrl(publication.getFichierPdfPath());
        if (pdfUrl == null) {
            throw new AuthException(AppErrorType.INVALID_FILE,
                    "Publication has no PDF file: " + publicationId);
        }

        PaperRequest request = new PaperRequest(pdfUrl);
        PaperApiResponse response = restTemplate.postForObject(
                aiServerUrl + "/paper", request, PaperApiResponse.class);

        if (response == null || !"paper".equals(response.type())) {
            String msg = response != null ? response.message() : "No response from AI server";
            throw new AuthException(AppErrorType.INVALID_FILE,
                    "AI paper extraction failed: " + msg);
        }

        PaperPayload payload = response.payload();
        return new AiPaperResponse(payload.title(), payload.resume());
    }

    @Transactional(readOnly = true)
    public AiNewsArticleResponse generateNewsArticle(UUID publicationId) {
        PublicationEntity publication = publicationRepository.findByIdWithAuthors(publicationId)
                .orElseThrow(() -> new AuthException(AppErrorType.PUBLICATION_NOT_FOUND,
                        "Publication not found: " + publicationId));

        List<ResearcherInput> researchers = publication.getAuteurs().stream()
                .map(ep -> ep.getChercheur())
                .map(c -> new ResearcherInput(c.getName(), c.getBiographie()))
                .toList();

        NewsArticleRequest request = new NewsArticleRequest(
                publication.getTitre(), publication.getResume(), researchers);
        NewsArticleApiResponse response = restTemplate.postForObject(
                aiServerUrl + "/news-article", request, NewsArticleApiResponse.class);

        if (response == null || !"news_article".equals(response.type())) {
            String msg = response != null ? response.message() : "No response from AI server";
            throw new AuthException(AppErrorType.INVALID_ACTUALITE_CONTENT,
                    "AI news article generation failed: " + msg);
        }

        NewsArticlePayload payload = response.payload();
        return new AiNewsArticleResponse(payload.headline(), payload.article());
    }
}
