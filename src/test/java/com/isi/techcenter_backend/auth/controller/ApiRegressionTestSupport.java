package com.isi.techcenter_backend.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.isi.techcenter_backend.entity.UserRole;
import com.isi.techcenter_backend.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiRegressionTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected ResultActions getWithToken(String path, String token) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(path);
        if (token != null) {
            requestBuilder.header("Authorization", bearerToken(token));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions postJson(String path, String token, String payload) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
        if (token != null) {
            requestBuilder.header("Authorization", bearerToken(token));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions putJson(String path, String token, String payload) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
        if (token != null) {
            requestBuilder.header("Authorization", bearerToken(token));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions deleteWithToken(String path, String token) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete(path);
        if (token != null) {
            requestBuilder.header("Authorization", bearerToken(token));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected String signupAndLogin(String email, String username, String password) throws Exception {
        String signupPayload = """
                {
                  "email": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(email, username, password);

        postJson("/signup", null, signupPayload);
        return loginAndGetToken(email, password);
    }

    protected String signupLoginAndPromoteToAdmin(String email, String username, String password) throws Exception {
        signupAndLogin(email, username, password);
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        return loginAndGetToken(email, password);
    }

    protected String signupLoginAndPromoteToModerator(String email, String username, String password) throws Exception {
        signupAndLogin(email, username, password);
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setRole(UserRole.MODERATOR);
        userRepository.save(user);
        ensureModeratorProfileForEmail(email);
        return loginAndGetToken(email, password);
    }

    protected void ensureModeratorProfileForEmail(String email) {
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from moderateurs where user_id = ?",
                Integer.class,
                user.getUserId());
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("insert into moderateurs(user_id) values (?)", user.getUserId());
    }

    protected String loginAndGetToken(String identifier, String password) throws Exception {
        String loginPayload = """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(identifier, password);

        MvcResult loginResult = postJson("/login", null, loginPayload)
                .andReturn();

        return extractJsonField(loginResult, "accessToken");
    }

    protected String extractJsonField(MvcResult result, String fieldName) throws Exception {
        Object value = JsonParserFactory.getJsonParser()
                .parseMap(result.getResponse().getContentAsString())
                .get(fieldName);
        return value == null ? null : value.toString();
    }

    protected String bearerToken(String token) {
        return "Bearer " + token;
    }

    protected String randomEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    protected String randomUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}