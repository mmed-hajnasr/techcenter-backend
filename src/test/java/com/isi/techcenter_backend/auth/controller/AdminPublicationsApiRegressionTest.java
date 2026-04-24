package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import com.isi.techcenter_backend.entity.ChercheurEntity;
import com.isi.techcenter_backend.repository.ChercheurRepository;

class AdminPublicationsApiRegressionTest extends ApiRegressionTestSupport {

        @Autowired
        private ChercheurRepository chercheurRepository;

        @Test
        void adminCanCreateListUpdateAndDeletePublicationsWithAuthors() throws Exception {
                String adminToken = signupLoginAndPromoteToAdmin(
                                randomEmail("publications-admin"),
                                randomUsername("publications-admin"),
                                "password123");

                ChercheurEntity firstResearcher = new ChercheurEntity();
                firstResearcher.setName("Author One");
                firstResearcher = chercheurRepository.save(firstResearcher);

                ChercheurEntity secondResearcher = new ChercheurEntity();
                secondResearcher.setName("Author Two");
                secondResearcher = chercheurRepository.save(secondResearcher);

                String createPayload = """
                                {
                                  "titre": "Applied AI Publication",
                                  "resume": "A publication summary",
                                  "doi": "10.1000/test-doi",
                                  "datePublication": "2026-04-22T10:15:30Z",
                                  "researcherIds": ["%s", "%s"]
                                }
                                """.formatted(firstResearcher.getChercheurId(), secondResearcher.getChercheurId());

                MvcResult createResult = postJson("/admin/publications", adminToken, createPayload)
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.publicationId").isNotEmpty())
                                .andExpect(jsonPath("$.titre").value("Applied AI Publication"))
                                .andExpect(jsonPath("$.authors.length()").value(2))
                                .andExpect(jsonPath("$.authors[?(@.researcherId=='" + firstResearcher.getChercheurId()
                                                + "')]")
                                                .isNotEmpty())
                                .andExpect(jsonPath("$.authors[?(@.researcherId=='" + secondResearcher.getChercheurId()
                                                + "')]")
                                                .isNotEmpty())
                                .andReturn();

                String publicationId = extractJsonField(createResult, "publicationId");

                getWithToken("/admin/publications", adminToken)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[?(@.publicationId=='" + publicationId + "')]").isNotEmpty())
                                .andExpect(jsonPath("$[?(@.titre=='Applied AI Publication')]").isNotEmpty());

                String updatePayload = """
                                {
                                  "titre": "Updated Publication",
                                  "resume": "Updated summary",
                                  "doi": "10.1000/test-doi-updated",
                                  "datePublication": "2026-04-23T11:00:00Z",
                                  "researcherIds": ["%s"]
                                }
                                """.formatted(secondResearcher.getChercheurId());

                putJson("/admin/publications/%s".formatted(publicationId), adminToken, updatePayload)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titre").value("Updated Publication"))
                                .andExpect(jsonPath("$.authors.length()").value(1))
                                .andExpect(jsonPath("$.authors[0].researcherId")
                                                .value(secondResearcher.getChercheurId().toString()));

                deleteWithToken("/admin/publications/%s".formatted(publicationId), adminToken)
                                .andExpect(status().isNoContent());

                putJson("/admin/publications/%s".formatted(publicationId), adminToken, updatePayload)
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("PUBLICATION_NOT_FOUND"));
        }

        @Test
        void nonAdminCanListButCannotManagePublications() throws Exception {
                String userToken = signupAndLogin(
                                randomEmail("non-admin-publications"),
                                randomUsername("non-admin-publications"),
                                "password123");

                getWithToken("/admin/publications", userToken)
                                .andExpect(status().isOk());

                String createPayload = """
                                {
                                  "titre": "Unauthorized publication",
                                  "resume": "Should fail",
                                  "researcherIds": []
                                }
                                """;

                postJson("/admin/publications", userToken, createPayload)
                                .andExpect(status().isForbidden());
        }
}