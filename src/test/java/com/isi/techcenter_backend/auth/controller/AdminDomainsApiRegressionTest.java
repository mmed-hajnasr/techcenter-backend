package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class AdminDomainsApiRegressionTest extends ApiRegressionTestSupport {

    @Test
    void adminCanCreateListUpdateAndDeleteDomains() throws Exception {
        String adminToken = signupLoginAndPromoteToAdmin(
                randomEmail("domains-admin"),
                randomUsername("domains-admin-user"),
                "password123");

        String createPayload = """
                {
                  "name": "Artificial Intelligence",
                  "description": "AI and ML research"
                }
                """;

        MvcResult createResult = postJson("/admin/domains", adminToken, createPayload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domainId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Artificial Intelligence"))
                .andExpect(jsonPath("$.description").value("AI and ML research"))
                .andReturn();

        String domainId = extractJsonField(createResult, "domainId");

        getWithToken("/admin/domains", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.domainId=='" + domainId + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='Artificial Intelligence')]").isNotEmpty());

        String updatePayload = """
                {
                  "name": "Applied AI",
                  "description": "Applied AI projects"
                }
                """;

        putJson("/admin/domains/%s".formatted(domainId), adminToken, updatePayload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Applied AI"))
                .andExpect(jsonPath("$.description").value("Applied AI projects"));

        deleteWithToken("/admin/domains/%s".formatted(domainId), adminToken)
                .andExpect(status().isNoContent());

        putJson("/admin/domains/%s".formatted(domainId), adminToken, updatePayload)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOMAIN_NOT_FOUND"));
    }

    @Test
    void nonAdminCannotManageDomains() throws Exception {
        String userToken = signupAndLogin(
                randomEmail("domains-user"),
                randomUsername("domains-user"),
                "password123");

        String createPayload = """
                {
                  "name": "Robotics",
                  "description": "Robotics domain"
                }
                """;

        postJson("/admin/domains", userToken, createPayload)
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotCreateDuplicateDomainName() throws Exception {
        String adminToken = signupLoginAndPromoteToAdmin(
                randomEmail("domains-duplicate"),
                randomUsername("domains-duplicate-admin"),
                "password123");

        String payload = """
                {
                  "name": "Cybersecurity",
                  "description": "Security domain"
                }
                """;

        postJson("/admin/domains", adminToken, payload)
                .andExpect(status().isCreated());

        postJson("/admin/domains", adminToken, payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DOMAIN_NAME_ALREADY_EXISTS"));
    }
}