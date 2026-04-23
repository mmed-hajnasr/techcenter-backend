package com.isi.techcenter_backend.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.DomaineEntity;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.DomainResponse;
import com.isi.techcenter_backend.model.DomainUpsertRequest;
import com.isi.techcenter_backend.repository.DomaineRepository;

@Service
public class DomainAdminService {

    private final DomaineRepository domaineRepository;
    private final Tracer tracer;

    public DomainAdminService(DomaineRepository domaineRepository, Tracer tracer) {
        this.domaineRepository = domaineRepository;
        this.tracer = tracer;
    }

    @Transactional(readOnly = true)
    public List<DomainResponse> listDomains() {
        return inStep(
                "admin.domains.db-list",
                () -> domaineRepository.findAll(Sort.by(Sort.Direction.ASC, "nom"))
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                "step",
                "db-list-domains");
    }

    @Transactional
    public DomainResponse createDomain(DomainUpsertRequest request) {
        String normalizedName = normalizeName(request.name());
        boolean nameExists = inStep(
                "admin.domains.db-check-name-exists",
                () -> domaineRepository.existsByNomIgnoreCase(normalizedName),
                "step",
                "db-check-name-exists",
                "domainName",
                normalizedName);
        if (nameExists) {
            throw new AuthException(AppErrorType.DOMAIN_NAME_ALREADY_EXISTS, "Domain name already exists");
        }

        DomaineEntity domain = new DomaineEntity();
        domain.setNom(normalizedName);
        domain.setDescription(normalizeDescription(request.description()));

        DomaineEntity savedDomain = inStep(
                "admin.domains.db-save",
                () -> domaineRepository.save(domain),
                "step",
                "db-save-domain",
                "domainName",
                normalizedName);

        return toResponse(savedDomain);
    }

    @Transactional
    public DomainResponse updateDomain(UUID domainId, DomainUpsertRequest request) {
        String normalizedName = normalizeName(request.name());
        boolean nameExists = inStep(
                "admin.domains.db-check-name-exists-for-update",
                () -> domaineRepository.existsByNomIgnoreCaseAndDomaineIdNot(normalizedName, domainId),
                "step",
                "db-check-name-exists-for-update",
                "domainId",
                domainId.toString(),
                "domainName",
                normalizedName);
        if (nameExists) {
            throw new AuthException(AppErrorType.DOMAIN_NAME_ALREADY_EXISTS, "Domain name already exists");
        }

        DomaineEntity existingDomain = findDomain(domainId);
        existingDomain.setNom(normalizedName);
        existingDomain.setDescription(normalizeDescription(request.description()));

        DomaineEntity savedDomain = inStep(
                "admin.domains.db-save-update",
                () -> domaineRepository.save(existingDomain),
                "step",
                "db-save-update-domain",
                "domainId",
                domainId.toString(),
                "domainName",
                normalizedName);

        return toResponse(savedDomain);
    }

    @Transactional
    public void deleteDomain(UUID domainId) {
        DomaineEntity existingDomain = findDomain(domainId);
        inStep(
                "admin.domains.db-delete",
                () -> {
                    domaineRepository.delete(existingDomain);
                    return null;
                },
                "step",
                "db-delete-domain",
                "domainId",
                domainId.toString());
    }

    private DomaineEntity findDomain(UUID domainId) {
        return inStep(
                "admin.domains.db-find-by-id",
                () -> domaineRepository.findById(domainId)
                        .orElseThrow(() -> new AuthException(AppErrorType.DOMAIN_NOT_FOUND, "Domain not found")),
                "step",
                "db-find-domain-by-id",
                "domainId",
                domainId.toString());
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.length() < 2) {
            throw new AuthException(AppErrorType.INVALID_DOMAIN_NAME, "Domain name must be at least 2 characters");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DomainResponse toResponse(DomaineEntity domain) {
        return new DomainResponse(domain.getDomaineId(), domain.getNom(), domain.getDescription());
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
