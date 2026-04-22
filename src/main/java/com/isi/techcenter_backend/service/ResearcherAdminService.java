package com.isi.techcenter_backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.ChercheurEntity;
import com.isi.techcenter_backend.entity.DomaineEntity;
import com.isi.techcenter_backend.entity.SpecialiseDansEntity;
import com.isi.techcenter_backend.entity.SpecialiseDansId;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.DomainResponse;
import com.isi.techcenter_backend.model.ResearcherAdminResponse;
import com.isi.techcenter_backend.model.ResearcherUpdateRequest;
import com.isi.techcenter_backend.repository.ChercheurRepository;
import com.isi.techcenter_backend.repository.DomaineRepository;
import com.isi.techcenter_backend.repository.SpecialiseDansRepository;

@Service
public class ResearcherAdminService {

    private final ChercheurRepository chercheurRepository;
    private final DomaineRepository domaineRepository;
    private final SpecialiseDansRepository specialiseDansRepository;

    public ResearcherAdminService(
            ChercheurRepository chercheurRepository,
            DomaineRepository domaineRepository,
            SpecialiseDansRepository specialiseDansRepository) {
        this.chercheurRepository = chercheurRepository;
        this.domaineRepository = domaineRepository;
        this.specialiseDansRepository = specialiseDansRepository;
    }

    @Transactional(readOnly = true)
    public List<ResearcherAdminResponse> listResearchers(String name, List<UUID> domainIds) {
        Set<UUID> requiredDomainIds = normalizeDomainIds(domainIds);

        return chercheurRepository.searchForAdminByName(normalizeFilter(name))
                .stream()
                .filter(researcher -> hasAllRequiredDomains(researcher, requiredDomainIds))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ResearcherAdminResponse createResearcher(ResearcherUpdateRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedName = request.name().trim();

        if (normalizedName.length() < 3) {
            throw new AuthException(AppErrorType.INVALID_USERNAME, "Name must be at least 3 characters");
        }

        if (chercheurRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AuthException(AppErrorType.INVALID_EMAIL, "Email is already in use");
        }

        ChercheurEntity researcher = new ChercheurEntity();
        researcher.setEmail(normalizedEmail);
        researcher.setName(normalizedName);
        researcher.setBiographie(normalizeBiography(request.biographie()));
        ChercheurEntity savedResearcher = chercheurRepository.save(researcher);

        if (request.domainIds() != null) {
            updateResearcherDomains(savedResearcher, request.domainIds());
        }

        return toResponse(findResearcherWithSpecialisations(savedResearcher.getChercheurId()));
    }

    @Transactional
    public ResearcherAdminResponse updateResearcher(UUID researcherId, ResearcherUpdateRequest request) {
        ChercheurEntity researcher = findResearcherWithSpecialisations(researcherId);

        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedName = request.name().trim();

        if (normalizedName.length() < 3) {
            throw new AuthException(AppErrorType.INVALID_USERNAME, "Name must be at least 3 characters");
        }

        if (chercheurRepository.existsByEmailIgnoreCaseAndChercheurIdNot(normalizedEmail, researcherId)) {
            throw new AuthException(AppErrorType.INVALID_EMAIL, "Email is already in use");
        }

        researcher.setEmail(normalizedEmail);
        researcher.setName(normalizedName);
        researcher.setBiographie(normalizeBiography(request.biographie()));
        chercheurRepository.save(researcher);

        if (request.domainIds() != null) {
            updateResearcherDomains(researcher, request.domainIds());
        }

        return toResponse(findResearcherWithSpecialisations(researcherId));
    }

    @Transactional
    public void deleteResearcher(UUID researcherId) {
        ChercheurEntity researcher = findResearcherWithSpecialisations(researcherId);
        specialiseDansRepository.deleteByChercheur_ChercheurId(researcherId);
        chercheurRepository.delete(researcher);
    }

    private void updateResearcherDomains(ChercheurEntity researcher, List<UUID> domainIds) {
        Set<UUID> uniqueDomainIds = new LinkedHashSet<>(domainIds);
        List<DomaineEntity> domains = domaineRepository.findAllById(uniqueDomainIds);

        if (domains.size() != uniqueDomainIds.size()) {
            throw new AuthException(AppErrorType.DOMAIN_NOT_FOUND, "One or more domains were not found");
        }

        specialiseDansRepository.deleteByChercheur_ChercheurId(researcher.getChercheurId());

        if (domains.isEmpty()) {
            researcher.setSpecialisations(new ArrayList<>());
            return;
        }

        List<SpecialiseDansEntity> specialisations = domains.stream()
                .map(domain -> {
                    SpecialiseDansEntity specialisation = new SpecialiseDansEntity();
                    SpecialiseDansId id = new SpecialiseDansId();
                    id.setChercheurId(researcher.getChercheurId());
                    id.setDomaineId(domain.getDomaineId());
                    specialisation.setId(id);
                    specialisation.setChercheur(researcher);
                    specialisation.setDomaine(domain);
                    specialisation.setDateAssociation(OffsetDateTime.now());
                    return specialisation;
                })
                .toList();

        specialiseDansRepository.saveAll(specialisations);
        researcher.setSpecialisations(new ArrayList<>(specialisations));
    }

    private ChercheurEntity findResearcherWithSpecialisations(UUID researcherId) {
        return chercheurRepository.findByIdWithSpecialisations(researcherId)
                .orElseThrow(() -> new AuthException(AppErrorType.RESEARCHER_NOT_FOUND, "Researcher not found"));
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Set<UUID> normalizeDomainIds(List<UUID> domainIds) {
        if (domainIds == null) {
            return Set.of();
        }

        return new LinkedHashSet<>(domainIds);
    }

    private boolean hasAllRequiredDomains(ChercheurEntity researcher, Set<UUID> requiredDomainIds) {
        if (requiredDomainIds.isEmpty()) {
            return true;
        }

        Set<UUID> researcherDomainIds = researcher.getSpecialisations()
                .stream()
                .map(specialisation -> specialisation.getDomaine().getDomaineId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        return researcherDomainIds.containsAll(requiredDomainIds);
    }

    private String normalizeBiography(String biography) {
        if (biography == null) {
            return null;
        }

        String trimmed = biography.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ResearcherAdminResponse toResponse(ChercheurEntity researcher) {
        List<DomainResponse> domains = researcher.getSpecialisations()
                .stream()
                .map(specialisation -> specialisation.getDomaine())
                .distinct()
                .map(domain -> new DomainResponse(domain.getDomaineId(), domain.getNom(), domain.getDescription()))
                .sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
                .toList();

        return new ResearcherAdminResponse(
                researcher.getChercheurId(),
                researcher.getEmail(),
                researcher.getName(),
                researcher.getBiographie(),
                domains,
                researcher.getCreatedAt());
    }
}
