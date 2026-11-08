package com.university.management.service;

import com.university.management.dto.request.AdminUpdateRequest;
import com.university.management.dto.response.AdminResponse;
import com.university.management.dto.response.DashboardResponse;
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

/**
 * Extended unit tests for AdminService, supplementing AdminServiceTest.
 * Covers update, dashboard, and additional edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Extended Unit Tests")
class AdminServiceExtendedTest {

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

    // ─── Update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAdmin: should update fullName and department in-place")
    void updateAdmin_updatesFields() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setFullName("Admin Updated");
        request.setDepartment("Engineering");

        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(adminRepository.save(any())).thenReturn(sampleAdmin);

        adminService.updateAdmin(1L, request);

        verify(adminRepository).save(argThat(a ->
                a.getFullName().equals("Admin Updated") &&
                a.getDepartment().equals("Engineering")
        ));
    }

    @Test
    @DisplayName("updateAdmin: should throw DuplicateResourceException for email already in use")
    void updateAdmin_duplicateEmail_throws() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setEmail("taken@example.com");

        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(userAccountRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.updateAdmin(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@example.com");
    }

    @Test
    @DisplayName("updateAdmin: should hash new password when provided")
    void updateAdmin_newPassword_isHashed() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setPassword("NewSecurePass123!");

        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("$2a$12$newHash");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(adminRepository.save(any())).thenReturn(sampleAdmin);

        adminService.updateAdmin(1L, request);

        verify(passwordEncoder).encode("NewSecurePass123!");
        verify(userAccountRepository).save(argThat(ua -> ua.getPasswordHash().equals("$2a$12$newHash")));
    }

    @Test
    @DisplayName("updateAdmin: should throw BusinessException when disabling last active admin")
    void updateAdmin_disableLastAdmin_throws() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setEnabled(false);

        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(adminRepository.countActiveAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> adminService.updateAdmin(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("last active Admin");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAdmin: should allow disabling when multiple active admins exist")
    void updateAdmin_disableWhenMultipleAdmins_succeeds() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setEnabled(false);

        when(adminRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleAdmin));
        when(adminRepository.countActiveAdmins()).thenReturn(3L);
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(adminRepository.save(any())).thenReturn(sampleAdmin);

        assertThatNoException().isThrownBy(() -> adminService.updateAdmin(1L, request));
        verify(userAccountRepository).save(argThat(ua -> !ua.isEnabled()));
    }

    @Test
    @DisplayName("updateAdmin: should throw ResourceNotFoundException when admin not found")
    void updateAdmin_notFound_throws() {
        when(adminRepository.findByIdWithUserAccount(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateAdmin(999L, new AdminUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboard: should aggregate counts correctly")
    void getDashboard_returnsAggregatedCounts() {
        when(userAccountRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userAccountRepository.countByRole(Role.TEACHER)).thenReturn(5L);
        when(userAccountRepository.countByRole(Role.STUDENT)).thenReturn(50L);
        when(adminRepository.countActiveAdmins()).thenReturn(2L);

        DashboardResponse response = adminService.getDashboard();

        assertThat(response.getTotalAdmins()).isEqualTo(2L);
        assertThat(response.getTotalTeachers()).isEqualTo(5L);
        assertThat(response.getTotalStudents()).isEqualTo(50L);
        assertThat(response.getTotalUsers()).isEqualTo(57L); // 2 + 5 + 50
        assertThat(response.getActiveAdmins()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getDashboard: totalUsers equals sum of all role counts")
    void getDashboard_totalUsersIsSumOfRoles() {
        when(userAccountRepository.countByRole(Role.ADMIN)).thenReturn(3L);
        when(userAccountRepository.countByRole(Role.TEACHER)).thenReturn(7L);
        when(userAccountRepository.countByRole(Role.STUDENT)).thenReturn(100L);
        when(adminRepository.countActiveAdmins()).thenReturn(3L);

        DashboardResponse response = adminService.getDashboard();

        assertThat(response.getTotalUsers())
                .isEqualTo(response.getTotalAdmins() + response.getTotalTeachers() + response.getTotalStudents());
    }
}

