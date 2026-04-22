package com.isi.techcenter_backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.ActualiteEntity;
import com.isi.techcenter_backend.entity.ModerateurEntity;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.ActualiteModeratorResponse;
import com.isi.techcenter_backend.model.ActualiteUpsertRequest;
import com.isi.techcenter_backend.repository.ActualiteRepository;
import com.isi.techcenter_backend.repository.ModerateurRepository;

@Service
public class ActualiteModeratorService {

    private final ActualiteRepository actualiteRepository;
    private final ModerateurRepository moderateurRepository;

    public ActualiteModeratorService(
            ActualiteRepository actualiteRepository,
            ModerateurRepository moderateurRepository) {
        this.actualiteRepository = actualiteRepository;
        this.moderateurRepository = moderateurRepository;
    }

    @Transactional
    public ActualiteModeratorResponse createActualite(UUID moderatorId, ActualiteUpsertRequest request) {
        ModerateurEntity moderator = findModeratorById(moderatorId);

        ActualiteEntity actualite = new ActualiteEntity();
        actualite.setTitre(normalizeTitle(request.titre()));
        actualite.setContenu(normalizeContent(request.contenu()));
        actualite.setDatePublication(resolveDatePublication(request.datePublication()));
        actualite.setEstEnAvant(Boolean.TRUE.equals(request.estEnAvant()));
        actualite.setModerateur(moderator);

        return toResponse(actualiteRepository.save(actualite));
    }

    @Transactional
    public ActualiteModeratorResponse updateActualite(UUID actualiteId, ActualiteUpsertRequest request) {
        ActualiteEntity actualite = findActualiteById(actualiteId);

        actualite.setTitre(normalizeTitle(request.titre()));
        actualite.setContenu(normalizeContent(request.contenu()));
        actualite.setDatePublication(resolveDatePublication(request.datePublication()));
        actualite.setEstEnAvant(Boolean.TRUE.equals(request.estEnAvant()));

        return toResponse(actualiteRepository.save(actualite));
    }

    @Transactional
    public void deleteActualite(UUID actualiteId) {
        ActualiteEntity actualite = findActualiteById(actualiteId);
        actualiteRepository.delete(actualite);
    }

    private ModerateurEntity findModeratorById(UUID moderatorId) {
        return moderateurRepository.findById(moderatorId)
                .orElseThrow(() -> new AuthException(AppErrorType.MODERATOR_NOT_FOUND, "Moderator not found"));
    }

    private ActualiteEntity findActualiteById(UUID actualiteId) {
        return actualiteRepository.findById(actualiteId)
                .orElseThrow(() -> new AuthException(AppErrorType.ACTUALITE_NOT_FOUND, "Actualite not found"));
    }

    private OffsetDateTime resolveDatePublication(OffsetDateTime datePublication) {
        return datePublication == null ? OffsetDateTime.now() : datePublication;
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.length() < 2) {
            throw new AuthException(AppErrorType.INVALID_PUBLICATION_TITLE,
                    "Actualite title must be at least 2 characters");
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new AuthException(AppErrorType.INVALID_ACTUALITE_CONTENT,
                    "Actualite content must not be empty");
        }
        return normalized;
    }

    private ActualiteModeratorResponse toResponse(ActualiteEntity actualite) {
        return new ActualiteModeratorResponse(
                actualite.getActualiteId(),
                actualite.getTitre(),
                actualite.getContenu(),
                actualite.getDatePublication(),
                actualite.getEstEnAvant(),
                actualite.getModerateur().getUserId());
    }
}
