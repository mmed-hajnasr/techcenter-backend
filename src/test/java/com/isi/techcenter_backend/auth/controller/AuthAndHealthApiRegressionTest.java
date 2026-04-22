package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthAndHealthApiRegressionTest extends ApiRegressionTestSupport {

    @Test
    void healthEndpointIsPublicAndUp() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void signupThenLoginThenGetCurrentUser() throws Exception {
        String email = randomEmail("regression");
        String username = randomUsername("regression-user");
        String password = "password123";

        String signupPayload = """
                {
                  "email": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(email, username, password);

        MvcResult signupResult = postJson("/signup", null, signupPayload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = JsonParserFactory.getJsonParser()
                .parseMap(signupResult.getResponse().getContentAsString())
                .get("accessToken")
                .toString();

        String loginPayload = """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        postJson("/login", null, loginPayload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        getWithToken("/user/me", accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.expiresInSeconds").value(0));

        getWithToken("/admin/users", accessToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void signupWithDuplicateEmailReturnsValidationError() throws Exception {
        String email = randomEmail("duplicate");

        String firstSignupPayload = """
                {
                  "email": "%s",
                  "username": "first-user",
                  "password": "password123"
                }
                """.formatted(email);

        postJson("/signup", null, firstSignupPayload)
                .andExpect(status().isOk());

        String secondSignupPayload = """
                {
                  "email": "%s",
                  "username": "second-user",
                  "password": "password123"
                }
                """.formatted(email);

        postJson("/signup", null, secondSignupPayload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EMAIL"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String email = randomEmail("wrongpass");
        String username = randomUsername("wrongpass-user");

        String signupPayload = """
                {
                  "email": "%s",
                  "username": "%s",
                  "password": "password123"
                }
                """.formatted(email, username);

        postJson("/signup", null, signupPayload)
                .andExpect(status().isOk());

        String badLoginPayload = """
                {
                  "identifier": "%s",
                  "password": "bad-password"
                }
                """.formatted(email);

        postJson("/login", null, badLoginPayload)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INCORRECT_LOGIN"));
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/user/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}