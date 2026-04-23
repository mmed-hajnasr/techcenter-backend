package com.isi.techcenter_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.model.PublicationAdminResponse;
import com.isi.techcenter_backend.model.PublicationUpsertRequest;
import com.isi.techcenter_backend.service.PublicationAdminService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/publications")
public class PublicationAdminController {

    private final PublicationAdminService publicationAdminService;
    private final EndpointTraceSupport endpointTraceSupport;

    public PublicationAdminController(
            PublicationAdminService publicationAdminService,
            EndpointTraceSupport endpointTraceSupport) {
        this.publicationAdminService = publicationAdminService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @GetMapping
    public ResponseEntity<List<PublicationAdminResponse>> listPublications() {
        return endpointTraceSupport.inSpan(
                "admin.publications.list",
                "/admin/publications",
                "list-publications",
                () -> ResponseEntity.ok(publicationAdminService.listPublications()));
    }

    @PostMapping
    public ResponseEntity<PublicationAdminResponse> createPublication(
            @Valid @RequestBody PublicationUpsertRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.publications.create",
                "/admin/publications",
                "create-publication",
                () -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(publicationAdminService.createPublication(request)));
    }

    @PutMapping("/{publicationId}")
    public ResponseEntity<PublicationAdminResponse> updatePublication(
            @PathVariable UUID publicationId,
            @Valid @RequestBody PublicationUpsertRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.publications.update",
                "/admin/publications/{publicationId}",
                "update-publication",
                () -> ResponseEntity.ok(publicationAdminService.updatePublication(publicationId, request)),
                "publicationId",
                publicationId.toString());
    }

    @DeleteMapping("/{publicationId}")
    public ResponseEntity<Void> deletePublication(@PathVariable UUID publicationId) {
        return endpointTraceSupport.inSpan(
                "admin.publications.delete",
                "/admin/publications/{publicationId}",
                "delete-publication",
                () -> {
                    publicationAdminService.deletePublication(publicationId);
                    return ResponseEntity.noContent().build();
                },
                "publicationId",
                publicationId.toString());
    }
}
