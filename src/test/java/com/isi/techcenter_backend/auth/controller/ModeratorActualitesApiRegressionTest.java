package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class ModeratorActualitesApiRegressionTest extends ApiRegressionTestSupport {

        @Test
        void moderatorCanCreateUpdateAndDeleteActualites() throws Exception {
                String moderatorToken = signupLoginAndPromoteToModerator(
                                randomEmail("moderator"),
                                randomUsername("moderator"),
                                "password123");

                String createPayload = """
                                {
                                  "titre": "Nouvelle annonce de recherche",
                                  "contenu": "Appel a candidatures pour un nouveau projet IA.",
                                  "datePublication": "2026-04-22T10:00:00Z",
                                  "estEnAvant": true
                                }
                                """;

                MvcResult createResult = postJson("/moderator/actualites", moderatorToken, createPayload)
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.actualiteId").isNotEmpty())
                                .andExpect(jsonPath("$.titre").value("Nouvelle annonce de recherche"))
                                .andExpect(jsonPath("$.estEnAvant").value(true))
                                .andExpect(jsonPath("$.moderateurId").isNotEmpty())
                                .andExpect(jsonPath("$.photoUrl").isEmpty())
                                .andReturn();

                String actualiteId = extractJsonField(createResult, "actualiteId");

                String updatePayload = """
                                {
                                  "titre": "Annonce mise a jour",
                                  "contenu": "Contenu mis a jour de cette actualite.",
                                  "datePublication": "2026-04-23T09:30:00Z",
                                  "estEnAvant": false
                                }
                                """;

                putJson("/moderator/actualites/%s".formatted(actualiteId), moderatorToken, updatePayload)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.actualiteId").value(actualiteId))
                                .andExpect(jsonPath("$.titre").value("Annonce mise a jour"))
                                .andExpect(jsonPath("$.estEnAvant").value(false))
                                .andExpect(jsonPath("$.photoUrl").isEmpty());

                deleteWithToken("/moderator/actualites/%s".formatted(actualiteId), moderatorToken)
                                .andExpect(status().isNoContent());

                deleteWithToken("/moderator/actualites/%s".formatted(actualiteId), moderatorToken)
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("ACTUALITE_NOT_FOUND"));
        }

        @Test
        void adminCanManageActualites() throws Exception {
                String adminEmail = randomEmail("admin-actualites");
                String adminToken = signupLoginAndPromoteToAdmin(
                                adminEmail,
                                randomUsername("admin-actualites"),
                                "password123");
                ensureModeratorProfileForEmail(adminEmail);

                String createPayload = """
                                {
                                  "titre": "Annonce admin",
                                  "contenu": "Creation par admin",
                                  "estEnAvant": false
                                }
                                """;

                postJson("/moderator/actualites", adminToken, createPayload)
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.titre").value("Annonce admin"));
        }

        @Test
        void regularUserCannotManageActualites() throws Exception {
                String userToken = signupAndLogin(
                                randomEmail("actualites-user"),
                                randomUsername("actualites-user"),
                                "password123");

                String createPayload = """
                                {
                                  "titre": "Unauthorized post",
                                  "contenu": "Should fail",
                                  "estEnAvant": false
                                }
                                """;

                postJson("/moderator/actualites", userToken, createPayload)
                                .andExpect(status().isForbidden());
        }
}