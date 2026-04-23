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
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/researchers")
public class ResearcherAdminController {

    private final ResearcherAdminService researcherAdminService;
    private final EndpointTraceSupport endpointTraceSupport;

    public ResearcherAdminController(
            ResearcherAdminService researcherAdminService,
            EndpointTraceSupport endpointTraceSupport) {
        this.researcherAdminService = researcherAdminService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @GetMapping
    public ResponseEntity<List<ResearcherAdminResponse>> listResearchers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<UUID> domainIds) {
        return endpointTraceSupport.inSpan(
                "admin.researchers.list",
                "/admin/researchers",
                "list-researchers",
                () -> ResponseEntity.ok(researcherAdminService.listResearchers(name, domainIds)),
                "hasNameFilter",
                Boolean.toString(name != null && !name.isBlank()),
                "domainIdsCount",
                Integer.toString(domainIds == null ? 0 : domainIds.size()));
    }

    @PostMapping
    public ResponseEntity<ResearcherAdminResponse> createResearcher(
            @Valid @RequestBody ResearcherUpdateRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.researchers.create",
                "/admin/researchers",
                "create-researcher",
                () -> ResponseEntity.status(201).body(researcherAdminService.createResearcher(request)),
                "hasDomainAssignments",
                Boolean.toString(request.domainIds() != null && !request.domainIds().isEmpty()),
                "domainIdsCount",
                Integer.toString(request.domainIds() == null ? 0 : request.domainIds().size()));
    }

    @PutMapping("/{researcherId}")
    public ResponseEntity<ResearcherAdminResponse> updateResearcher(
            @PathVariable UUID researcherId,
            @Valid @RequestBody ResearcherUpdateRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.researchers.update",
                "/admin/researchers/{researcherId}",
                "update-researcher",
                () -> ResponseEntity.ok(researcherAdminService.updateResearcher(researcherId, request)),
                "researcherId",
                researcherId.toString(),
                "hasDomainAssignments",
                Boolean.toString(request.domainIds() != null),
                "domainIdsCount",
                Integer.toString(request.domainIds() == null ? 0 : request.domainIds().size()));
    }

    @DeleteMapping("/{researcherId}")
    public ResponseEntity<Void> deleteResearcher(@PathVariable UUID researcherId) {
        return endpointTraceSupport.inSpan(
                "admin.researchers.delete",
                "/admin/researchers/{researcherId}",
                "delete-researcher",
                () -> {
                    researcherAdminService.deleteResearcher(researcherId);
                    return ResponseEntity.noContent().build();
                },
                "researcherId",
                researcherId.toString());
    }
}
