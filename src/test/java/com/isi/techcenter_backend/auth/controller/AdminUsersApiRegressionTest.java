package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class AdminUsersApiRegressionTest extends ApiRegressionTestSupport {

    @Test
    void adminCanListUsersUpdateRoleAndDeleteUser() throws Exception {
        String adminToken = signupLoginAndPromoteToAdmin(
                randomEmail("users-admin"),
                randomUsername("users-admin"),
                "password123");

        String targetEmail = randomEmail("users-target");
        String targetUsername = randomUsername("users-target");
        signupAndLogin(targetEmail, targetUsername, "password123");

        String userId = userRepository.findByEmailIgnoreCase(targetEmail)
                .orElseThrow()
                .getUserId()
                .toString();

        getWithToken("/admin/users", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email").isArray())
                .andExpect(jsonPath("$[?(@.email=='" + targetEmail + "')]").isNotEmpty());

        String roleUpdatePayload = """
                {
                  "role": "MODERATOR"
                }
                """;

        putJson("/admin/users/%s/role".formatted(userId), adminToken, roleUpdatePayload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.role").value("MODERATOR"));

        deleteWithToken("/admin/users/%s".formatted(userId), adminToken)
                .andExpect(status().isNoContent());

        deleteWithToken("/admin/users/%s".formatted(userId), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        String userToken = signupAndLogin(
                randomEmail("non-admin-users"),
                randomUsername("non-admin-users"),
                "password123");

        getWithToken("/admin/users", userToken)
                .andExpect(status().isForbidden());
    }
}