package com.isi.techcenter_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.model.UserAdminResponse;
import com.isi.techcenter_backend.model.UserRoleUpdateRequest;
import com.isi.techcenter_backend.service.UserAdminService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final EndpointTraceSupport endpointTraceSupport;

    public UserAdminController(UserAdminService userAdminService, EndpointTraceSupport endpointTraceSupport) {
        this.userAdminService = userAdminService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @GetMapping
    public ResponseEntity<List<UserAdminResponse>> listUsers() {
        return endpointTraceSupport.inSpan(
                "admin.users.list",
                "/admin/users",
                "list-users",
                () -> ResponseEntity.ok(userAdminService.listUsers()));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserAdminResponse> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return endpointTraceSupport.inSpan(
                "admin.users.update-role",
                "/admin/users/{userId}/role",
                "update-user-role",
                () -> ResponseEntity.ok(userAdminService.updateUserRole(userId, request)),
                "userId",
                userId.toString(),
                "newRole",
                request.role().name());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        return endpointTraceSupport.inSpan(
                "admin.users.delete",
                "/admin/users/{userId}",
                "delete-user",
                () -> {
                    userAdminService.deleteUser(userId);
                    return ResponseEntity.noContent().build();
                },
                "userId",
                userId.toString());
    }
}
