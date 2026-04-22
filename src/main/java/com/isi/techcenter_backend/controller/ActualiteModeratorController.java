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

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/moderator/actualites")
public class ActualiteModeratorController {

    private final ActualiteModeratorService actualiteModeratorService;

    public ActualiteModeratorController(ActualiteModeratorService actualiteModeratorService) {
        this.actualiteModeratorService = actualiteModeratorService;
    }

    @PostMapping
    public ResponseEntity<ActualiteModeratorResponse> createActualite(
            Authentication authentication,
            @Valid @RequestBody ActualiteUpsertRequest request) {
        UUID moderatorId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(actualiteModeratorService.createActualite(moderatorId, request));
    }

    @PutMapping("/{actualiteId}")
    public ResponseEntity<ActualiteModeratorResponse> updateActualite(
            @PathVariable UUID actualiteId,
            @Valid @RequestBody ActualiteUpsertRequest request) {
        return ResponseEntity.ok(actualiteModeratorService.updateActualite(actualiteId, request));
    }

    @DeleteMapping("/{actualiteId}")
    public ResponseEntity<Void> deleteActualite(@PathVariable UUID actualiteId) {
        actualiteModeratorService.deleteActualite(actualiteId);
        return ResponseEntity.noContent().build();
    }
}
