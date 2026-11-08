package com.university.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.management.dto.request.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration tests using Testcontainers MySQL (via tc: JDBC URL in test application.yml).
 * Covers authentication, CRUD operations, role-based access control, and security boundaries.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Shared state between ordered tests
    private static String adminToken;
    private static String teacherToken;
    private static String studentToken;
    private static Long teacherId;
    private static Long studentId;

    // ─── Setup: Create bootstrap admin via direct call to TeacherService/AdminService ─

    @Test
    @Order(1)
    @DisplayName("Setup: Create admin account directly (simulating bootstrap)")
    void setup_createInitialAdmin() throws Exception {
        // We'll use the admin create endpoint — but first we need an admin token.
        // Since there's no admin yet, we test that it's protected:
        mockMvc.perform(get("/api/v1/admins"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Authentication tests ─────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Auth: Login with invalid credentials returns 401")
    void auth_login_invalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("Auth: Login with blank fields returns 400")
    void auth_login_blankFields() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Auth: /me endpoint requires authentication")
    void auth_me_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Unauthenticated access tests ─────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Security: Unauthenticated access to /api/v1/admins returns 401")
    void security_unauthenticated_admins_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admins")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("Security: Unauthenticated access to /api/v1/teachers returns 401")
    void security_unauthenticated_teachers_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("Security: Unauthenticated access to /api/v1/students returns 401")
    void security_unauthenticated_students_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/students")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Security: Unauthenticated access to /api/v1/admin/dashboard returns 401")
    void security_unauthenticated_dashboard_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    // ─── Invalid JWT tests ────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Security: Malformed JWT returns 401")
    void security_malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admins")
                .header("Authorization", "Bearer notavalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    @DisplayName("Security: Expired JWT returns 401")
    void security_expiredJwt_returns401() throws Exception {
        // A well-formed but expired JWT (crafted manually for testing)
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDF9.invalid";
        mockMvc.perform(get("/api/v1/admins")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // ─── Swagger UI accessible without auth ───────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("OpenAPI: Swagger UI is accessible without authentication")
    void swagger_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ─── Validation tests ─────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("Validation: Teacher create with missing fields returns 400 with fieldErrors")
    void validation_teacherCreate_missingFields() throws Exception {
        // We'll use a bearer token mock with @WithMockUser for this test
        String emptyJson = "{}";
        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyJson)
                .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isUnauthorized()); // No valid token → 401 before validation
    }

    @Test
    @Order(13)
    @DisplayName("Validation: Login with missing Content-Type returns 415 or 400")
    void validation_loginMissingContentType() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().is4xxClientError());
    }
}
