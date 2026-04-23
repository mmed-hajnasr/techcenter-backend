package com.isi.techcenter_backend.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.UserEntity;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.UserAdminResponse;
import com.isi.techcenter_backend.model.UserRoleUpdateRequest;
import com.isi.techcenter_backend.repository.UserRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final Tracer tracer;

    public UserAdminService(UserRepository userRepository, Tracer tracer) {
        this.userRepository = userRepository;
        this.tracer = tracer;
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> listUsers() {
        return inStep(
                "admin.users.db-find-all",
                () -> userRepository.findAll(Sort.by(Sort.Direction.ASC, "username")),
                "step",
                "find-all-users")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserAdminResponse updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        UserEntity user = findUser(userId);
        user.setRole(request.role());
        UserEntity savedUser = inStep(
                "admin.users.db-save",
                () -> userRepository.save(user),
                "step",
                "save-user",
                "userId",
                userId.toString(),
                "newRole",
                request.role().name());
        return toResponse(savedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = findUser(userId);
        inStep(
                "admin.users.db-delete",
                () -> {
                    userRepository.delete(user);
                    return null;
                },
                "step",
                "delete-user",
                "userId",
                userId.toString());
    }

    private UserEntity findUser(UUID userId) {
        return inStep(
                "admin.users.db-find-by-id",
                () -> userRepository.findById(userId)
                        .orElseThrow(() -> new AuthException(AppErrorType.USER_NOT_FOUND, "User not found")),
                "step",
                "find-user-by-id",
                "userId",
                userId.toString());
    }

    private UserAdminResponse toResponse(UserEntity user) {
        return new UserAdminResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt());
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
