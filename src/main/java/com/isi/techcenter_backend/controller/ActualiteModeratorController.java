package com.isi.techcenter_backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.model.ActualiteModeratorResponse;
import com.isi.techcenter_backend.model.ActualiteUpsertRequest;
import com.isi.techcenter_backend.service.ActualiteModeratorService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/moderator/actualites")
public class ActualiteModeratorController {

    private final ActualiteModeratorService actualiteModeratorService;
    private final EndpointTraceSupport endpointTraceSupport;

    public ActualiteModeratorController(
            ActualiteModeratorService actualiteModeratorService,
            EndpointTraceSupport endpointTraceSupport) {
        this.actualiteModeratorService = actualiteModeratorService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @PostMapping
    public ResponseEntity<ActualiteModeratorResponse> createActualite(
            Authentication authentication,
            @Valid @RequestBody ActualiteUpsertRequest request) {
        UUID moderatorId = UUID.fromString(authentication.getName());
        return endpointTraceSupport.inSpan(
                "moderator.actualites.create",
                "/moderator/actualites",
                "create-actualite",
                () -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(actualiteModeratorService.createActualite(moderatorId, request)),
                "moderatorId",
                moderatorId.toString());
    }

    @PutMapping("/{actualiteId}")
    public ResponseEntity<ActualiteModeratorResponse> updateActualite(
            Authentication authentication,
            @PathVariable UUID actualiteId,
            @Valid @RequestBody ActualiteUpsertRequest request) {
        UUID moderatorId = UUID.fromString(authentication.getName());
        return endpointTraceSupport.inSpan(
                "moderator.actualites.update",
                "/moderator/actualites/{actualiteId}",
                "update-actualite",
                () -> ResponseEntity.ok(actualiteModeratorService.updateActualite(actualiteId, request)),
                "actualiteId",
                actualiteId.toString(),
                "moderatorId",
                moderatorId.toString());
    }

    @DeleteMapping("/{actualiteId}")
    public ResponseEntity<Void> deleteActualite(
            Authentication authentication,
            @PathVariable UUID actualiteId) {
        UUID moderatorId = UUID.fromString(authentication.getName());
        return endpointTraceSupport.inSpan(
                "moderator.actualites.delete",
                "/moderator/actualites/{actualiteId}",
                "delete-actualite",
                () -> {
                    actualiteModeratorService.deleteActualite(actualiteId);
                    return ResponseEntity.noContent().build();
                },
                "actualiteId",
                actualiteId.toString(),
                "moderatorId",
                moderatorId.toString());
    }
}
