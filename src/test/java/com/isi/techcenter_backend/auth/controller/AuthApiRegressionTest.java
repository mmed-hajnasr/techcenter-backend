package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiRegressionTest {

    @Autowired
    private MockMvc mockMvc;

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
}
