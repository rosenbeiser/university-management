package com.university.management.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        private static final String SECURITY_SCHEME_NAME = "bearerAuth";

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("University Management System API")
                                                .description("""
                                                                Complete REST API for University Management with Role-Based Access Control.

                                                                ## Authentication
                                                                Use `POST /api/v1/auth/login` to obtain a JWT access token.
                                                                Click **Authorize** and enter: `Bearer <your-access-token>`

                                                                ## Roles & Permissions
                                                                - **ADMIN**: Full access to all resources. **Only ADMIN can register new students and appoint new faculty**.
                                                                - **TEACHER**: Can view student lists and faculty directory.
                                                                - **STUDENT**: Can view faculty directory and own profile.
                                                                """)
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("University Management System")
                                                                .email("admin@university.edu")))
                                .servers(List.of(
                                                new Server().url("http://localhost:8081")
                                                                .description("Local Development"),
                                                new Server().url("http://localhost:8081")
                                                                .description("Docker Compose")))
                                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                                .components(new Components()
                                                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                                                new SecurityScheme()
                                                                                .name(SECURITY_SCHEME_NAME)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Enter JWT token obtained from /api/v1/auth/login")));
        }
}
