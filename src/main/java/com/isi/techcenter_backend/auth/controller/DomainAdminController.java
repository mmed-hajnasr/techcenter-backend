package com.isi.techcenter_backend.auth.controller;

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

import com.isi.techcenter_backend.auth.model.DomainResponse;
import com.isi.techcenter_backend.auth.model.DomainUpsertRequest;
import com.isi.techcenter_backend.auth.service.DomainAdminService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/domains")
public class DomainAdminController {

    private final DomainAdminService domainAdminService;

    public DomainAdminController(DomainAdminService domainAdminService) {
        this.domainAdminService = domainAdminService;
    }

    @GetMapping
    public ResponseEntity<List<DomainResponse>> listDomains() {
        return ResponseEntity.ok(domainAdminService.listDomains());
    }

    @PostMapping
    public ResponseEntity<DomainResponse> createDomain(@Valid @RequestBody DomainUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(domainAdminService.createDomain(request));
    }

    @PutMapping("/{domainId}")
    public ResponseEntity<DomainResponse> updateDomain(
            @PathVariable UUID domainId,
            @Valid @RequestBody DomainUpsertRequest request) {
        return ResponseEntity.ok(domainAdminService.updateDomain(domainId, request));
    }

    @DeleteMapping("/{domainId}")
    public ResponseEntity<Void> deleteDomain(@PathVariable UUID domainId) {
        domainAdminService.deleteDomain(domainId);
        return ResponseEntity.noContent().build();
    }
}
