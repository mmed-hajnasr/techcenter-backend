package com.isi.techcenter_backend.auth.controller;

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

import com.isi.techcenter_backend.auth.model.UserAdminResponse;
import com.isi.techcenter_backend.auth.model.UserRoleUpdateRequest;
import com.isi.techcenter_backend.auth.service.UserAdminService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<List<UserAdminResponse>> listUsers() {
        return ResponseEntity.ok(userAdminService.listUsers());
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserAdminResponse> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return ResponseEntity.ok(userAdminService.updateUserRole(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
