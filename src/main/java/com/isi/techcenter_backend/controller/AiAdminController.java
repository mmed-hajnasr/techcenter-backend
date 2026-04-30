package com.isi.techcenter_backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.model.AiBiographyResponse;
import com.isi.techcenter_backend.model.AiNewsArticleResponse;
import com.isi.techcenter_backend.model.AiPaperResponse;
import com.isi.techcenter_backend.service.AiAdminService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

@RestController
@Validated
@RequestMapping("/admin/ai")
public class AiAdminController {

    private final AiAdminService aiAdminService;
    private final EndpointTraceSupport endpointTraceSupport;

    public AiAdminController(AiAdminService aiAdminService, EndpointTraceSupport endpointTraceSupport) {
        this.aiAdminService = aiAdminService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @PostMapping("/researchers/{researcherId}/biography")
    public ResponseEntity<AiBiographyResponse> generateBiography(@PathVariable UUID researcherId) {
        return endpointTraceSupport.inSpan(
                "admin.ai.biography",
                "/admin/ai/researchers/{researcherId}/biography",
                "generate-biography",
                () -> ResponseEntity.ok(aiAdminService.generateBiography(researcherId)),
                "researcherId", researcherId.toString());
    }

    @PostMapping("/publications/{publicationId}/paper")
    public ResponseEntity<AiPaperResponse> generatePaper(@PathVariable UUID publicationId) {
        return endpointTraceSupport.inSpan(
                "admin.ai.paper",
                "/admin/ai/publications/{publicationId}/paper",
                "generate-paper",
                () -> ResponseEntity.ok(aiAdminService.generatePaper(publicationId)),
                "publicationId", publicationId.toString());
    }

    @PostMapping("/publications/{publicationId}/news-article")
    public ResponseEntity<AiNewsArticleResponse> generateNewsArticle(@PathVariable UUID publicationId) {
        return endpointTraceSupport.inSpan(
                "admin.ai.news-article",
                "/admin/ai/publications/{publicationId}/news-article",
                "generate-news-article",
                () -> ResponseEntity.ok(aiAdminService.generateNewsArticle(publicationId)),
                "publicationId", publicationId.toString());
    }
}
