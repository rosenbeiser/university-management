package com.university.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.management.dto.request.LoginRequest;
import com.university.management.dto.request.TeacherCreateRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Teacher CRUD endpoints, RBAC, and validation.
 * Runs against a real Testcontainers MySQL instance via the "test" profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TeacherCrudIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ─── Unauthenticated access ───────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("IT-Teacher-01: GET /api/v1/teachers without auth returns 401")
    void getTeachers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("IT-Teacher-02: POST /api/v1/teachers without auth returns 401")
    void createTeacher_unauthenticated_returns401() throws Exception {
        TeacherCreateRequest req = new TeacherCreateRequest();
        req.setUsername("teacher_new");
        req.setEmail("teacher_new@example.com");
        req.setPassword("SecurePass123!");
        req.setFullName("New Teacher");
        req.setDepartment("CS");

        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("IT-Teacher-03: PUT /api/v1/teachers/{id} without auth returns 401")
    void updateTeacher_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/teachers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("IT-Teacher-04: DELETE /api/v1/teachers/{id} without auth returns 401")
    void deleteTeacher_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("IT-Teacher-05: GET /api/v1/teachers/{id} with malformed JWT returns 401")
    void getTeacherById_malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/1")
                .header("Authorization", "Bearer this.is.garbage"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("IT-Teacher-06: POST /api/v1/auth/login with invalid credentials returns 401")
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("nonexistent");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("IT-Teacher-07: POST /api/v1/auth/login with blank body returns 400 with fieldErrors")
    void login_blankBody_returns400WithFieldErrors() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("");
        req.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @Order(8)
    @DisplayName("IT-Teacher-08: GET /api/v1/admin/dashboard without auth returns 401")
    void dashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("IT-Teacher-09: Swagger UI accessible without authentication")
    void swaggerUi_accessible_without_auth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("IT-Teacher-10: POST /api/v1/auth/login missing Content-Type returns 4xx")
    void login_missingContentType_returns4xx() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                .andExpect(status().is4xxClientError());
    }
}

