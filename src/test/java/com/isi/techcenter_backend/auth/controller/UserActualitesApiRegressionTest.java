package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class UserActualitesApiRegressionTest extends ApiRegressionTestSupport {

    @Test
    void authenticatedUserCanListActualites() throws Exception {
        String moderatorToken = signupLoginAndPromoteToModerator(
                randomEmail("mod-ua"),
                randomUsername("mod-ua"),
                "password123");

        String createPayload = """
                {
                  "titre": "Annonce publique",
                  "contenu": "Contenu visible pour les utilisateurs.",
                  "datePublication": "2026-04-22T10:00:00Z",
                  "estEnAvant": true
                }
                """;

        MvcResult createResult = postJson("/moderator/actualites", moderatorToken, createPayload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actualiteId").isNotEmpty())
                .andReturn();

        String createdActualiteId = extractJsonField(createResult, "actualiteId");

        String userToken = signupAndLogin(
                randomEmail("user-ua"),
                randomUsername("user-ua"),
                "password123");

        getWithToken("/user/actualites", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actualiteId").isNotEmpty())
                .andExpect(jsonPath("$[0].titre").isNotEmpty())
                .andExpect(jsonPath("$[0].contenu").isNotEmpty())
                .andExpect(jsonPath("$[0].estEnAvant").isBoolean())
                .andExpect(jsonPath("$[0].moderateurId").isNotEmpty())
                .andExpect(jsonPath("$[?(@.actualiteId == '%s')]".formatted(createdActualiteId)).isArray());
    }

    @Test
    void listActualitesWithoutTokenReturnsUnauthorized() throws Exception {
        getWithToken("/user/actualites", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
