package com.university.management.service;

import com.university.management.dto.request.AdminCreateRequest;
import com.university.management.dto.response.AdminResponse;
import com.university.management.entity.Admin;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.BusinessException;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminService adminService;

    private UserAccount sampleAccount;
    private Admin sampleAdmin;

    @BeforeEach
    void setUp() {
        sampleAccount = UserAccount.builder()
                .id(1L)
                .username("admin01")
                .email("admin01@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        sampleAdmin = Admin.builder()
                .id(1L)
                .userAccount(sampleAccount)
                .fullName("Admin User")
                .department("IT")
                .build();
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAdmin: should create admin with ADMIN role and hashed password")
    void createAdmin_success() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setUsername("admin01");
        request.setEmail("admin01@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Admin User");
        request.setDepartment("IT");

        when(userAccountRepository.existsByUsername("admin01")).thenReturn(false);
        when(userAccountRepository.existsByEmail("admin01@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$12$hashedPassword");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(adminRepository.save(any())).thenReturn(sampleAdmin);

        AdminResponse response = adminService.createAdmin(request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Admin User");
        assertThat(response.getAccount().getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getAccount().getUsername()).isEqualTo("admin01");

        verify(passwordEncoder).encode("SecurePass123!");
        verify(userAccountRepository).save(argThat(ua -> ua.getRole() == Role.ADMIN));
    }

    @Test
    @DisplayName("createAdmin: should throw DuplicateResourceException for duplicate username")
    void createAdmin_duplicateUsername() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setUsername("admin01");
        request.setEmail("new@example.com");
        request.setPassword("pass");
        request.setFullName("Name");

        when(userAccountRepository.existsByUsername("admin01")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin01");
    }

    @Test
    @DisplayName("createAdmin: should throw DuplicateResourceException for duplicate email")
    void createAdmin_duplicateEmail() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setUsername("newadmin");
        request.setEmail("admin01@example.com");
        request.setPassword("pass");
        request.setFullName("Name");

        when(userAccountRepository.existsByUsername("newadmin")).thenReturn(false);
        when(userAccountRepository.existsByEmail("admin01@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin01@example.com");
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAdminById: should return admin when found")
    void getAdminById_found() {
        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));

        AdminResponse response = adminService.getAdminById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Admin User");
    }

    @Test
    @DisplayName("getAdminById: should throw ResourceNotFoundException when not found")
    void getAdminById_notFound() {
        when(adminRepository.findByIdWithUserAccount(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getAdminById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAdmin: should prevent deleting the last active admin")
    void deleteAdmin_lastAdmin_throws() {
        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(adminRepository.countActiveAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> adminService.deleteAdmin(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("last active Admin");
    }

    @Test
    @DisplayName("deleteAdmin: should succeed when more than one active admin exists")
    void deleteAdmin_success_multipleAdmins() {
        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(adminRepository.countActiveAdmins()).thenReturn(2L);

        adminService.deleteAdmin(1L);

        verify(userAccountRepository).delete(sampleAccount);
    }

    // ─── Password safety ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createAdmin: response should never contain password hash")
    void createAdmin_responseDoesNotContainPassword() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setUsername("admin01");
        request.setEmail("admin01@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Admin User");

        when(userAccountRepository.existsByUsername(any())).thenReturn(false);
        when(userAccountRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashedPassword");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(adminRepository.save(any())).thenReturn(sampleAdmin);

        AdminResponse response = adminService.createAdmin(request);

        // AdminResponse has no password field — just verify via reflection that it's absent
        assertThat(response.toString()).doesNotContain("password");
        assertThat(response.toString()).doesNotContain("hash");
    }
}
