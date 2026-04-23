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

import com.isi.techcenter_backend.model.DomainResponse;
import com.isi.techcenter_backend.model.DomainUpsertRequest;
import com.isi.techcenter_backend.service.DomainAdminService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/domains")
public class DomainAdminController {

    private final DomainAdminService domainAdminService;
    private final EndpointTraceSupport endpointTraceSupport;

    public DomainAdminController(DomainAdminService domainAdminService, EndpointTraceSupport endpointTraceSupport) {
        this.domainAdminService = domainAdminService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @GetMapping
    public ResponseEntity<List<DomainResponse>> listDomains() {
        return endpointTraceSupport.inSpan(
                "admin.domains.list",
                "/admin/domains",
                "list-domains",
                () -> ResponseEntity.ok(domainAdminService.listDomains()));
    }

    @PostMapping
    public ResponseEntity<DomainResponse> createDomain(@Valid @RequestBody DomainUpsertRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.domains.create",
                "/admin/domains",
                "create-domain",
                () -> ResponseEntity.status(HttpStatus.CREATED).body(domainAdminService.createDomain(request)));
    }

    @PutMapping("/{domainId}")
    public ResponseEntity<DomainResponse> updateDomain(
            @PathVariable UUID domainId,
            @Valid @RequestBody DomainUpsertRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.domains.update",
                "/admin/domains/{domainId}",
                "update-domain",
                () -> ResponseEntity.ok(domainAdminService.updateDomain(domainId, request)),
                "domainId",
                domainId.toString());
    }

    @DeleteMapping("/{domainId}")
    public ResponseEntity<Void> deleteDomain(@PathVariable UUID domainId) {
        return endpointTraceSupport.inSpan(
                "admin.domains.delete",
                "/admin/domains/{domainId}",
                "delete-domain",
                () -> {
                    domainAdminService.deleteDomain(domainId);
                    return ResponseEntity.noContent().build();
                },
                "domainId",
                domainId.toString());
    }
}
