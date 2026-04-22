package com.isi.techcenter_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.model.ResearcherAdminResponse;
import com.isi.techcenter_backend.model.ResearcherUpdateRequest;
import com.isi.techcenter_backend.service.ResearcherAdminService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/researchers")
public class ResearcherAdminController {

    private final ResearcherAdminService researcherAdminService;

    public ResearcherAdminController(ResearcherAdminService researcherAdminService) {
        this.researcherAdminService = researcherAdminService;
    }

    @GetMapping
    public ResponseEntity<List<ResearcherAdminResponse>> listResearchers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<UUID> domainIds) {
        return ResponseEntity.ok(researcherAdminService.listResearchers(name, domainIds));
    }

    @PostMapping
    public ResponseEntity<ResearcherAdminResponse> createResearcher(
            @Valid @RequestBody ResearcherUpdateRequest request) {
        return ResponseEntity.status(201).body(researcherAdminService.createResearcher(request));
    }

    @PutMapping("/{researcherId}")
    public ResponseEntity<ResearcherAdminResponse> updateResearcher(
            @PathVariable UUID researcherId,
            @Valid @RequestBody ResearcherUpdateRequest request) {
        return ResponseEntity.ok(researcherAdminService.updateResearcher(researcherId, request));
    }

    @DeleteMapping("/{researcherId}")
    public ResponseEntity<Void> deleteResearcher(@PathVariable UUID researcherId) {
        researcherAdminService.deleteResearcher(researcherId);
        return ResponseEntity.noContent().build();
    }
}
