package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.isi.techcenter_backend.entity.DomaineEntity;
import com.isi.techcenter_backend.entity.ChercheurEntity;
import com.isi.techcenter_backend.entity.UserRole;
import com.isi.techcenter_backend.repository.ChercheurRepository;
import com.isi.techcenter_backend.repository.DomaineRepository;
import com.isi.techcenter_backend.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Replaced by split regression suites: AuthAndHealth/AdminDomains/AdminUsers/AdminResearchers/AdminPublications/ModeratorActualites")
class AuthApiRegressionTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ChercheurRepository chercheurRepository;

  @Autowired
  private DomaineRepository domaineRepository;

  @Test
  void healthEndpointIsPublicAndUp() throws Exception {
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void signupThenLoginThenGetCurrentUser() throws Exception {
    String signupPayload = """
        {
          "email": "regression@example.com",
          "username": "regression-user",
          "password": "password123"
        }
        """;

    MvcResult signupResult = mockMvc.perform(post("/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(signupPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.email").value("regression@example.com"))
        .andExpect(jsonPath("$.username").value("regression-user"))
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andReturn();

    String accessToken = JsonParserFactory.getJsonParser()
        .parseMap(signupResult.getResponse().getContentAsString())
        .get("accessToken")
        .toString();

    String loginPayload = """
        {
          "identifier": "regression@example.com",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("regression@example.com"))
        .andExpect(jsonPath("$.accessToken").isNotEmpty());

    mockMvc.perform(get("/user/me")
        .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("regression@example.com"))
        .andExpect(jsonPath("$.username").value("regression-user"))
        .andExpect(jsonPath("$.accessToken").doesNotExist())
        .andExpect(jsonPath("$.expiresInSeconds").value(0));

    mockMvc.perform(get("/admin/users")
        .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanAccessAdminEndpoints() throws Exception {
    String signupPayload = """
        {
          "email": "admin@example.com",
          "username": "admin-user",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(signupPayload))
        .andExpect(status().isOk());

    var adminUser = userRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow();
    adminUser.setRole(UserRole.ADMIN);
    userRepository.save(adminUser);

    String loginPayload = """
        {
          "identifier": "admin@example.com",
          "password": "password123"
        }
        """;

    MvcResult loginResult = mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginPayload))
        .andExpect(status().isOk())
        .andReturn();

    String adminToken = JsonParserFactory.getJsonParser()
        .parseMap(loginResult.getResponse().getContentAsString())
        .get("accessToken")
        .toString();

    mockMvc.perform(get("/admin/users")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void signupWithDuplicateEmailReturnsValidationError() throws Exception {
    String firstSignupPayload = """
        {
          "email": "duplicate@example.com",
          "username": "first-user",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(firstSignupPayload))
        .andExpect(status().isOk());

    String secondSignupPayload = """
        {
          "email": "duplicate@example.com",
          "username": "second-user",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(secondSignupPayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_EMAIL"));
  }

  @Test
  void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
    String signupPayload = """
        {
          "email": "wrongpass@example.com",
          "username": "wrongpass-user",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(signupPayload))
        .andExpect(status().isOk());

    String badLoginPayload = """
        {
          "identifier": "wrongpass@example.com",
          "password": "bad-password"
        }
        """;

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(badLoginPayload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("INCORRECT_LOGIN"));
  }

  @Test
  void meWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/user/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void adminCanCreateListUpdateAndDeleteDomains() throws Exception {
    String adminToken = signupLoginAndPromoteToAdmin("domains-admin@example.com", "domains-admin-user");

    String createPayload = """
        {
          "name": "Artificial Intelligence",
          "description": "AI and ML research"
        }
        """;

    MvcResult createResult = mockMvc.perform(post("/admin/domains")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.domainId").isNotEmpty())
        .andExpect(jsonPath("$.name").value("Artificial Intelligence"))
        .andExpect(jsonPath("$.description").value("AI and ML research"))
        .andReturn();

    String domainId = JsonParserFactory.getJsonParser()
        .parseMap(createResult.getResponse().getContentAsString())
        .get("domainId")
        .toString();

    mockMvc.perform(get("/admin/domains")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.domainId=='" + domainId + "')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.name=='Artificial Intelligence')]").isNotEmpty());

    String updatePayload = """
        {
          "name": "Applied AI",
          "description": "Applied AI projects"
        }
        """;

    mockMvc.perform(put("/admin/domains/{domainId}", domainId)
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Applied AI"))
        .andExpect(jsonPath("$.description").value("Applied AI projects"));

    mockMvc.perform(delete("/admin/domains/{domainId}", domainId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/admin/domains/{domainId}", domainId)
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatePayload))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("DOMAIN_NOT_FOUND"));
  }

  @Test
  void nonAdminCannotManageDomains() throws Exception {
    String userToken = signupAndLogin("domains-user@example.com", "domains-user", "password123");

    String createPayload = """
        {
          "name": "Robotics",
          "description": "Robotics domain"
        }
        """;

    mockMvc.perform(post("/admin/domains")
        .header("Authorization", "Bearer " + userToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPayload))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCannotCreateDuplicateDomainName() throws Exception {
    String adminToken = signupLoginAndPromoteToAdmin("domains-duplicate@example.com", "domains-duplicate-admin");

    String payload = """
        {
          "name": "Cybersecurity",
          "description": "Security domain"
        }
        """;

    mockMvc.perform(post("/admin/domains")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/admin/domains")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("DOMAIN_NAME_ALREADY_EXISTS"));
  }

  @Test
  void adminCanListUsersUpdateRoleAndDeleteUser() throws Exception {
    String adminToken = signupLoginAndPromoteToAdmin("users-admin@example.com", "users-admin");
    signupAndLogin("users-target@example.com", "users-target", "password123");

    String userId = userRepository.findByEmailIgnoreCase("users-target@example.com")
        .orElseThrow()
        .getUserId()
        .toString();

    mockMvc.perform(get("/admin/users")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].email").isArray())
        .andExpect(jsonPath("$[?(@.email=='users-target@example.com')]").isNotEmpty());

    String roleUpdatePayload = """
        {
          "role": "MODERATOR"
        }
        """;

    mockMvc.perform(put("/admin/users/{userId}/role", userId)
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(roleUpdatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.role").value("MODERATOR"));

    mockMvc.perform(delete("/admin/users/{userId}", userId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(delete("/admin/users/{userId}", userId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
  }

  @Test
  void adminCanListResearchersFilterUpdateAndDelete() throws Exception {
    String adminToken = signupLoginAndPromoteToAdmin("research-admin@example.com", "research-admin");

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
          "email": "researcher1@example.com",
          "biographie": "Initial bio",
          "domainIds": ["%s"]
        }
        """.formatted(aiDomain.getDomaineId());

    MvcResult createdFirstResearcher = mockMvc.perform(post("/admin/researchers")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(createFirstPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("alice researcher"))
        .andReturn();

    String firstResearcherId = JsonParserFactory.getJsonParser()
        .parseMap(createdFirstResearcher.getResponse().getContentAsString())
        .get("researcherId")
        .toString();

    String createSecondPayload = """
        {
          "name": "bob researcher",
          "email": "researcher2@example.com",
          "biographie": "Second bio",
          "domainIds": ["%s", "%s"]
        }
        """.formatted(aiDomain.getDomaineId(), roboticsDomain.getDomaineId());

    MvcResult createdSecondResearcher = mockMvc.perform(post("/admin/researchers")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(createSecondPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("researcher2@example.com"))
        .andExpect(jsonPath("$.name").value("bob researcher"))
        .andReturn();

    String secondResearcherId = JsonParserFactory.getJsonParser()
        .parseMap(createdSecondResearcher.getResponse().getContentAsString())
        .get("researcherId")
        .toString();

    mockMvc.perform(get("/admin/researchers")
        .header("Authorization", "Bearer " + adminToken)
        .param("name", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("researcher1@example.com"))
        .andExpect(jsonPath("$[0].domains[0].name").value("AI Research Domain"));

    mockMvc.perform(get("/admin/researchers")
        .header("Authorization", "Bearer " + adminToken)
        .param("domainIds", aiDomain.getDomaineId().toString(), roboticsDomain.getDomaineId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].researcherId").value(secondResearcherId));

    String updatePayload = """
        {
        "name": "alice updated",
          "email": "researcher1-updated@example.com",
          "biographie": "Updated bio",
          "domainIds": ["%s"]
        }
        """.formatted(roboticsDomain.getDomaineId());

    mockMvc.perform(put("/admin/researchers/{researcherId}", firstResearcherId)
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("alice updated"))
        .andExpect(jsonPath("$.email").value("researcher1-updated@example.com"))
        .andExpect(jsonPath("$.domains[0].name").value("Robotics"));

    mockMvc.perform(delete("/admin/researchers/{researcherId}", firstResearcherId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(delete("/admin/researchers/{researcherId}", firstResearcherId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("RESEARCHER_NOT_FOUND"));

    var secondResearcher = chercheurRepository.findById(java.util.UUID.fromString(secondResearcherId)).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals("bob researcher", secondResearcher.getName());
  }

  @Test
  void nonAdminCannotManageUsersOrResearchers() throws Exception {
    String userToken = signupAndLogin("non-admin@example.com", "non-admin", "password123");

    mockMvc.perform(get("/admin/users")
        .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/admin/researchers")
        .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanCreateListUpdateAndDeletePublicationsWithAuthors() throws Exception {
    String adminToken = signupLoginAndPromoteToAdmin("publications-admin@example.com", "publications-admin");

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

    MvcResult createResult = mockMvc.perform(post("/admin/publications")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicationId").isNotEmpty())
        .andExpect(jsonPath("$.titre").value("Applied AI Publication"))
        .andExpect(jsonPath("$.authors.length()").value(2))
        .andExpect(jsonPath("$.authors[?(@.researcherId=='" + firstResearcher.getChercheurId() + "')] ").isNotEmpty())
        .andExpect(jsonPath("$.authors[?(@.researcherId=='" + secondResearcher.getChercheurId() + "')] ").isNotEmpty())
        .andReturn();

    String publicationId = JsonParserFactory.getJsonParser()
        .parseMap(createResult.getResponse().getContentAsString())
        .get("publicationId")
        .toString();

    mockMvc.perform(get("/admin/publications")
        .header("Authorization", "Bearer " + adminToken))
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

    mockMvc.perform(put("/admin/publications/{publicationId}", publicationId)
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titre").value("Updated Publication"))
        .andExpect(jsonPath("$.authors.length()").value(1))
        .andExpect(jsonPath("$.authors[0].researcherId").value(secondResearcher.getChercheurId().toString()));

    mockMvc.perform(delete("/admin/publications/{publicationId}", publicationId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/admin/publications/{publicationId}", publicationId)
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatePayload))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("PUBLICATION_NOT_FOUND"));
  }

  private String signupLoginAndPromoteToAdmin(String email, String username) throws Exception {
    signupAndLogin(email, username, "password123");
    var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
    user.setRole(UserRole.ADMIN);
    userRepository.save(user);

    return loginAndGetToken(email, "password123");
  }

  private String signupAndLogin(String email, String username, String password) throws Exception {
    String signupPayload = """
        {
          "email": "%s",
          "username": "%s",
          "password": "%s"
        }
        """.formatted(email, username, password);

    mockMvc.perform(post("/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(signupPayload))
        .andExpect(status().isOk());

    return loginAndGetToken(email, password);
  }

  private String loginAndGetToken(String identifier, String password) throws Exception {
    String loginPayload = """
        {
          "identifier": "%s",
          "password": "%s"
        }
        """.formatted(identifier, password);

    MvcResult loginResult = mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginPayload))
        .andExpect(status().isOk())
        .andReturn();

    return JsonParserFactory.getJsonParser()
        .parseMap(loginResult.getResponse().getContentAsString())
        .get("accessToken")
        .toString();
  }
}
