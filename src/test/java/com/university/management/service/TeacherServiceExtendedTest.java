package com.university.management.service;

import com.university.management.dto.request.TeacherUpdateRequest;
import com.university.management.dto.response.PageResponse;
import com.university.management.dto.response.TeacherResponse;
import com.university.management.entity.Teacher;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.TeacherRepository;
import com.university.management.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Extended unit tests for TeacherService, supplementing the existing TeacherServiceTest.
 * Covers update, pagination, and additional edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherService Extended Unit Tests")
class TeacherServiceExtendedTest {

    @Mock private TeacherRepository teacherRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private TeacherService teacherService;

    private UserAccount sampleAccount;
    private Teacher sampleTeacher;

    @BeforeEach
    void setUp() {
        sampleAccount = UserAccount.builder()
                .id(1L)
                .username("teacher01")
                .email("teacher01@example.com")
                .passwordHash("$2a$12$hashed")
                .role(Role.TEACHER)
                .enabled(true)
                .build();

        sampleTeacher = Teacher.builder()
                .id(1L)
                .userAccount(sampleAccount)
                .fullName("Alex Morgan")
                .department("Computer Science")
                .designation("Lecturer")
                .phone("01700000000")
                .build();
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTeacherById: should return correctly mapped response")
    void getTeacherById_found_returnsMappedResponse() {
        when(teacherRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleTeacher));

        TeacherResponse response = teacherService.getTeacherById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Alex Morgan");
        assertThat(response.getDepartment()).isEqualTo("Computer Science");
        assertThat(response.getDesignation()).isEqualTo("Lecturer");
        assertThat(response.getAccount().getRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("getAllTeachers: should return paged response with correct metadata")
    void getAllTeachers_returnsPaginatedResult() {
        Page<Teacher> page = new PageImpl<>(List.of(sampleTeacher), PageRequest.of(0, 10), 1);
        when(teacherRepository.findAllWithUserAccount(any(Pageable.class))).thenReturn(page);

        PageResponse<TeacherResponse> result = teacherService.getAllTeachers(0, 10, "id", "asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.isLast()).isTrue();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTeacher: should update fullName, department, and designation")
    void updateTeacher_updatesFields() {
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setFullName("Alex Updated");
        request.setDepartment("Mathematics");
        request.setDesignation("Associate Professor");

        when(teacherRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleTeacher));
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(teacherRepository.save(any())).thenReturn(sampleTeacher);

        teacherService.updateTeacher(1L, request);

        verify(teacherRepository).save(argThat(t ->
                t.getFullName().equals("Alex Updated") &&
                t.getDepartment().equals("Mathematics") &&
                t.getDesignation().equals("Associate Professor")
        ));
    }

    @Test
    @DisplayName("updateTeacher: should throw DuplicateResourceException for email already in use")
    void updateTeacher_duplicateEmail_throws() {
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setEmail("taken@example.com");

        when(teacherRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleTeacher));
        when(userAccountRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.updateTeacher(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@example.com");
    }

    @Test
    @DisplayName("updateTeacher: same email should not trigger duplicate check")
    void updateTeacher_sameEmail_doesNotThrow() {
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setEmail("teacher01@example.com"); // same as existing

        when(teacherRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleTeacher));
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(teacherRepository.save(any())).thenReturn(sampleTeacher);

        assertThatNoException().isThrownBy(() -> teacherService.updateTeacher(1L, request));
        verify(userAccountRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("updateTeacher: should hash new password when provided")
    void updateTeacher_newPassword_isHashed() {
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setPassword("NewSecurePass123!");

        when(teacherRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleTeacher));
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("$2a$12$newHash");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(teacherRepository.save(any())).thenReturn(sampleTeacher);

        teacherService.updateTeacher(1L, request);

        verify(passwordEncoder).encode("NewSecurePass123!");
        verify(userAccountRepository).save(argThat(ua -> ua.getPasswordHash().equals("$2a$12$newHash")));
    }

    @Test
    @DisplayName("updateTeacher: should throw ResourceNotFoundException when teacher not found")
    void updateTeacher_notFound_throws() {
        when(teacherRepository.findByIdWithUserAccount(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.updateTeacher(999L, new TeacherUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createTeacher: response should never expose password hash")
    void createTeacher_responseDoesNotContainPassword() {
        // This test reuses the happy-path setup from TeacherServiceTest but validates security
        com.university.management.dto.request.TeacherCreateRequest request =
                new com.university.management.dto.request.TeacherCreateRequest();
        request.setUsername("teacher01");
        request.setEmail("teacher01@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Alex Morgan");
        request.setDepartment("CS");
        request.setDesignation("Lecturer");

        when(userAccountRepository.existsByUsername(any())).thenReturn(false);
        when(userAccountRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(teacherRepository.save(any())).thenReturn(sampleTeacher);

        TeacherResponse response = teacherService.createTeacher(request);

        assertThat(response.toString()).doesNotContain("password");
        assertThat(response.toString()).doesNotContain("hash");
    }
}

