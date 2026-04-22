package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import com.isi.techcenter_backend.entity.DomaineEntity;
import com.isi.techcenter_backend.repository.ChercheurRepository;
import com.isi.techcenter_backend.repository.DomaineRepository;

class AdminResearchersApiRegressionTest extends ApiRegressionTestSupport {

    @Autowired
    private ChercheurRepository chercheurRepository;

    @Autowired
    private DomaineRepository domaineRepository;

    @Test
    void adminCanListResearchersFilterUpdateAndDelete() throws Exception {
        String adminToken = signupLoginAndPromoteToAdmin(
                randomEmail("research-admin"),
                randomUsername("research-admin"),
                "password123");

        DomaineEntity aiDomain = new DomaineEntity();
        aiDomain.setNom("AI Research Domain");
        aiDomain.setDescription("AI domain");
        aiDomain = domaineRepository.save(aiDomain);

        DomaineEntity roboticsDomain = new DomaineEntity();
        roboticsDomain.setNom("Robotics");
        roboticsDomain.setDescription("Robotics domain");
        roboticsDomain = domaineRepository.save(roboticsDomain);

        String createFirstPayload = """
                {
                  "name": "alice researcher",
                  "biographie": "Initial bio",
                  "domainIds": ["%s"]
                }
                                                                """.formatted(aiDomain.getDomaineId());

        MvcResult createdFirstResearcher = postJson("/admin/researchers", adminToken, createFirstPayload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("alice researcher"))
                .andReturn();

        String firstResearcherId = extractJsonField(createdFirstResearcher, "researcherId");

        String createSecondPayload = """
                {
                  "name": "bob researcher",
                  "biographie": "Second bio",
                  "domainIds": ["%s", "%s"]
                }
                                                                """.formatted(aiDomain.getDomaineId(),
                roboticsDomain.getDomaineId());

        MvcResult createdSecondResearcher = postJson("/admin/researchers", adminToken, createSecondPayload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("bob researcher"))
                .andReturn();

        String secondResearcherId = extractJsonField(createdSecondResearcher, "researcherId");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/researchers")
                .header("Authorization", bearerToken(adminToken))
                .param("name", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("alice researcher"))
                .andExpect(jsonPath("$[0].domains[0].name").value("AI Research Domain"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/researchers")
                .header("Authorization", bearerToken(adminToken))
                .param("domainIds", aiDomain.getDomaineId().toString(), roboticsDomain.getDomaineId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].researcherId").value(secondResearcherId));

        String updatePayload = """
                {
                  "name": "alice updated",
                  "biographie": "Updated bio",
                  "domainIds": ["%s"]
                }
                                                                """.formatted(roboticsDomain.getDomaineId());

        putJson("/admin/researchers/%s".formatted(firstResearcherId), adminToken, updatePayload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("alice updated"))
                .andExpect(jsonPath("$.domains[0].name").value("Robotics"));

        deleteWithToken("/admin/researchers/%s".formatted(firstResearcherId), adminToken)
                .andExpect(status().isNoContent());

        deleteWithToken("/admin/researchers/%s".formatted(firstResearcherId), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESEARCHER_NOT_FOUND"));

        var secondResearcher = chercheurRepository.findById(java.util.UUID.fromString(secondResearcherId))
                .orElseThrow();
        Assertions.assertEquals("bob researcher", secondResearcher.getName());
    }

    @Test
    void nonAdminCannotManageResearchers() throws Exception {
        String userToken = signupAndLogin(
                randomEmail("non-admin-researchers"),
                randomUsername("non-admin-researchers"),
                "password123");

        getWithToken("/admin/researchers", userToken)
                .andExpect(status().isForbidden());
    }
}