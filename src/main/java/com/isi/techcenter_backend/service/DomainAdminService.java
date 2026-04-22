package com.isi.techcenter_backend.service;

import java.util.List;
import java.util.UUID;

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

    public DomainAdminService(DomaineRepository domaineRepository) {
        this.domaineRepository = domaineRepository;
    }

    @Transactional(readOnly = true)
    public List<DomainResponse> listDomains() {
        return domaineRepository.findAll(Sort.by(Sort.Direction.ASC, "nom"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DomainResponse createDomain(DomainUpsertRequest request) {
        String normalizedName = normalizeName(request.name());
        if (domaineRepository.existsByNomIgnoreCase(normalizedName)) {
            throw new AuthException(AppErrorType.DOMAIN_NAME_ALREADY_EXISTS, "Domain name already exists");
        }

        DomaineEntity domain = new DomaineEntity();
        domain.setNom(normalizedName);
        domain.setDescription(normalizeDescription(request.description()));

        return toResponse(domaineRepository.save(domain));
    }

    @Transactional
    public DomainResponse updateDomain(UUID domainId, DomainUpsertRequest request) {
        String normalizedName = normalizeName(request.name());
        if (domaineRepository.existsByNomIgnoreCaseAndDomaineIdNot(normalizedName, domainId)) {
            throw new AuthException(AppErrorType.DOMAIN_NAME_ALREADY_EXISTS, "Domain name already exists");
        }

        DomaineEntity existingDomain = findDomain(domainId);
        existingDomain.setNom(normalizedName);
        existingDomain.setDescription(normalizeDescription(request.description()));

        return toResponse(domaineRepository.save(existingDomain));
    }

    @Transactional
    public void deleteDomain(UUID domainId) {
        DomaineEntity existingDomain = findDomain(domainId);
        domaineRepository.delete(existingDomain);
    }

    private DomaineEntity findDomain(UUID domainId) {
        return domaineRepository.findById(domainId)
                .orElseThrow(() -> new AuthException(AppErrorType.DOMAIN_NOT_FOUND, "Domain not found"));
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
}
