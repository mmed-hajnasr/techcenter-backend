package com.isi.techcenter_backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
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
    private final Tracer tracer;

    public ActualiteModeratorService(
            ActualiteRepository actualiteRepository,
            ModerateurRepository moderateurRepository,
            Tracer tracer) {
        this.actualiteRepository = actualiteRepository;
        this.moderateurRepository = moderateurRepository;
        this.tracer = tracer;
    }

    @Transactional
    public ActualiteModeratorResponse createActualite(UUID moderatorId, ActualiteUpsertRequest request) {
        ModerateurEntity moderator = inStep(
                "moderator.actualites.create.db-find-moderator-by-id",
                () -> findModeratorById(moderatorId),
                "step",
                "find-moderator-by-id",
                "moderatorId",
                moderatorId.toString());

        ActualiteEntity actualite = new ActualiteEntity();
        actualite.setTitre(inStep(
                "moderator.actualites.create.normalize-title",
                () -> normalizeTitle(request.titre()),
                "step",
                "normalize-title"));
        actualite.setContenu(inStep(
                "moderator.actualites.create.normalize-content",
                () -> normalizeContent(request.contenu()),
                "step",
                "normalize-content"));
        actualite.setDatePublication(inStep(
                "moderator.actualites.create.resolve-publication-date",
                () -> resolveDatePublication(request.datePublication()),
                "step",
                "resolve-publication-date"));
        actualite.setEstEnAvant(inStep(
                "moderator.actualites.create.resolve-featured-flag",
                () -> Boolean.TRUE.equals(request.estEnAvant()),
                "step",
                "resolve-featured-flag"));
        actualite.setModerateur(moderator);

        ActualiteEntity savedActualite = inStep(
                "moderator.actualites.create.db-save-actualite",
                () -> actualiteRepository.save(actualite),
                "step",
                "save-actualite",
                "moderatorId",
                moderatorId.toString());

        return inStep(
                "moderator.actualites.create.map-response",
                () -> toResponse(savedActualite),
                "step",
                "map-response",
                "actualiteId",
                savedActualite.getActualiteId().toString());
    }

    @Transactional
    public ActualiteModeratorResponse updateActualite(UUID actualiteId, ActualiteUpsertRequest request) {
        ActualiteEntity actualite = inStep(
                "moderator.actualites.update.db-find-actualite-by-id",
                () -> findActualiteById(actualiteId),
                "step",
                "find-actualite-by-id",
                "actualiteId",
                actualiteId.toString());

        actualite.setTitre(inStep(
                "moderator.actualites.update.normalize-title",
                () -> normalizeTitle(request.titre()),
                "step",
                "normalize-title",
                "actualiteId",
                actualiteId.toString()));
        actualite.setContenu(inStep(
                "moderator.actualites.update.normalize-content",
                () -> normalizeContent(request.contenu()),
                "step",
                "normalize-content",
                "actualiteId",
                actualiteId.toString()));
        actualite.setDatePublication(inStep(
                "moderator.actualites.update.resolve-publication-date",
                () -> resolveDatePublication(request.datePublication()),
                "step",
                "resolve-publication-date",
                "actualiteId",
                actualiteId.toString()));
        actualite.setEstEnAvant(inStep(
                "moderator.actualites.update.resolve-featured-flag",
                () -> Boolean.TRUE.equals(request.estEnAvant()),
                "step",
                "resolve-featured-flag",
                "actualiteId",
                actualiteId.toString()));

        ActualiteEntity savedActualite = inStep(
                "moderator.actualites.update.db-save-actualite",
                () -> actualiteRepository.save(actualite),
                "step",
                "save-actualite",
                "actualiteId",
                actualiteId.toString());

        return inStep(
                "moderator.actualites.update.map-response",
                () -> toResponse(savedActualite),
                "step",
                "map-response",
                "actualiteId",
                actualiteId.toString());
    }

    @Transactional
    public void deleteActualite(UUID actualiteId) {
        ActualiteEntity actualite = inStep(
                "moderator.actualites.delete.db-find-actualite-by-id",
                () -> findActualiteById(actualiteId),
                "step",
                "find-actualite-by-id",
                "actualiteId",
                actualiteId.toString());

        inStep(
                "moderator.actualites.delete.db-delete-actualite",
                () -> {
                    actualiteRepository.delete(actualite);
                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        currentSpan.tag("actualite.deletion.result", "deleted");
                        currentSpan.event("moderator.actualites.delete.completed");
                    }
                    return null;
                },
                "step",
                "delete-actualite",
                "actualiteId",
                actualiteId.toString());
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
