package com.university.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.management.dto.request.LoginRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Auth endpoints and Admin API security.
 * Runs against a real Testcontainers MySQL instance via the "test" profile.
 *
 * <p>Covers:
 * <ul>
 *   <li>All protected endpoints return 401 without authentication.</li>
 *   <li>Auth endpoint validation (blank fields → 400, wrong creds → 401).</li>
 *   <li>JWT header manipulation (tampered / no "Bearer" prefix / expired).</li>
 *   <li>Public endpoints (Swagger UI, OpenAPI spec) remain accessible.</li>
 *   <li>POST /api/v1/auth/refresh with an invalid token returns 400.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthAndAdminIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ─── Unauthenticated access to every protected resource ──────────────────

    @Test
    @Order(1)
    @DisplayName("IT-Auth-01: GET /api/v1/admins without auth returns 401")
    void getAdmins_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("IT-Auth-02: GET /api/v1/teachers without auth returns 401")
    void getTeachers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("IT-Auth-03: GET /api/v1/students without auth returns 401")
    void getStudents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("IT-Auth-04: GET /api/v1/admin/dashboard without auth returns 401")
    void getDashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("IT-Auth-05: GET /api/v1/auth/me without auth returns 401")
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Auth endpoint validation ─────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("IT-Auth-06: POST /api/v1/auth/login with wrong credentials returns 401")
    void login_wrongCredentials_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("hacker");
        req.setPassword("totallyWrong!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("IT-Auth-07: POST /api/v1/auth/login with blank username/password returns 400")
    void login_blankFields_returns400WithFieldErrors() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("");
        req.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    // ─── JWT header manipulation ───────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("IT-Auth-08: Malformed JWT (not 3 parts) returns 401")
    void malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admins")
                .header("Authorization", "Bearer notavalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("IT-Auth-09: Expired JWT (well-formed but expired) returns 401")
    void expiredJwt_returns401() throws Exception {
        // A well-formed HS256 token that is expired (exp is in the past)
        String expired = "eyJhbGciOiJIUzI1NiJ9" +
                ".eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDF9" +
                ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        mockMvc.perform(get("/api/v1/admins")
                .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    @DisplayName("IT-Auth-10: Authorization header without 'Bearer ' prefix is rejected (401)")
    void authHeader_withoutBearerPrefix_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admins")
                .header("Authorization", "Token sometoken"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Refresh token ────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("IT-Auth-11: POST /api/v1/auth/refresh with invalid token returns 400")
    void refresh_invalidToken_returns400() throws Exception {
        String body = "{\"refreshToken\":\"definitely-not-a-real-uuid\"}";

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest()); // BusinessException → 400
    }

    @Test
    @Order(12)
    @DisplayName("IT-Auth-12: POST /api/v1/auth/refresh with blank token returns 400 (validation)")
    void refresh_blankToken_returns400() throws Exception {
        String body = "{\"refreshToken\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── Public endpoints ─────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("IT-Auth-13: Swagger UI is publicly accessible")
    void swaggerUi_publiclyAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(14)
    @DisplayName("IT-Auth-14: OpenAPI JSON spec is publicly accessible")
    void openApiSpec_publiclyAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").exists());
    }
}

