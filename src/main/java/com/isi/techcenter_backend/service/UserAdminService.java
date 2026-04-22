package com.isi.techcenter_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.entity.UserEntity;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.model.UserAdminResponse;
import com.isi.techcenter_backend.model.UserRoleUpdateRequest;
import com.isi.techcenter_backend.repository.UserRepository;

@Service
public class UserAdminService {

    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserAdminResponse updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        UserEntity user = findUser(userId);
        user.setRole(request.role());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = findUser(userId);
        userRepository.delete(user);
    }

    private UserEntity findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AppErrorType.USER_NOT_FOUND, "User not found"));
    }

    private UserAdminResponse toResponse(UserEntity user) {
        return new UserAdminResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt());
    }
}
