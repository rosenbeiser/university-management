package com.university.management.service;

import com.university.management.dto.request.StudentUpdateRequest;
import com.university.management.dto.response.PageResponse;
import com.university.management.dto.response.StudentResponse;
import com.university.management.entity.Student;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.StudentRepository;
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
 * Extended unit tests for StudentService, supplementing the existing StudentServiceTest.
 * Covers update, pagination, and additional edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Extended Unit Tests")
class StudentServiceExtendedTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private StudentService studentService;

    private UserAccount sampleAccount;
    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        sampleAccount = UserAccount.builder()
                .id(1L)
                .username("student01")
                .email("student01@example.com")
                .passwordHash("$2a$12$hashed")
                .role(Role.STUDENT)
                .enabled(true)
                .build();

        sampleStudent = Student.builder()
                .id(1L)
                .userAccount(sampleAccount)
                .fullName("Jordan Lee")
                .studentNumber("STU-2026-001")
                .department("Computer Science")
                .semester(1)
                .phone("01800000000")
                .build();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStudent: should update fullName and department in-place")
    void updateStudent_updatesFields() {
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setFullName("Jordan Updated");
        request.setDepartment("Mathematics");

        when(studentRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleStudent));
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(studentRepository.save(any())).thenReturn(sampleStudent);

        StudentResponse response = studentService.updateStudent(1L, request);

        // The service mutates the entity in place before saving — verify saves were called
        verify(studentRepository).save(argThat(s -> s.getFullName().equals("Jordan Updated")));
        verify(userAccountRepository).save(any());
    }

    @Test
    @DisplayName("updateStudent: should throw DuplicateResourceException for email already in use by another account")
    void updateStudent_duplicateEmail_throws() {
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setEmail("taken@example.com");

        when(studentRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleStudent));
        when(userAccountRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.updateStudent(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@example.com");
    }

    @Test
    @DisplayName("updateStudent: same email should not trigger duplicate check")
    void updateStudent_sameEmail_doesNotThrow() {
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setEmail("student01@example.com"); // same as existing

        when(studentRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleStudent));
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(studentRepository.save(any())).thenReturn(sampleStudent);

        assertThatNoException().isThrownBy(() -> studentService.updateStudent(1L, request));
        verify(userAccountRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("updateStudent: should hash new password when password is provided")
    void updateStudent_newPassword_isHashed() {
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setPassword("NewSecurePass123!");

        when(studentRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleStudent));
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("$2a$12$newHash");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(studentRepository.save(any())).thenReturn(sampleStudent);

        studentService.updateStudent(1L, request);

        verify(passwordEncoder).encode("NewSecurePass123!");
        verify(userAccountRepository).save(argThat(ua -> ua.getPasswordHash().equals("$2a$12$newHash")));
    }

    @Test
    @DisplayName("updateStudent: should throw ResourceNotFoundException when student does not exist")
    void updateStudent_notFound_throws() {
        when(studentRepository.findByIdWithUserAccount(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(999L, new StudentUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudentById: should return correctly mapped response")
    void getStudentById_found_returnsMappedResponse() {
        when(studentRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleStudent));

        StudentResponse response = studentService.getStudentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Jordan Lee");
        assertThat(response.getStudentNumber()).isEqualTo("STU-2026-001");
        assertThat(response.getDepartment()).isEqualTo("Computer Science");
        assertThat(response.getSemester()).isEqualTo(1);
        assertThat(response.getAccount().getUsername()).isEqualTo("student01");
        assertThat(response.getAccount().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("getAllStudents: should return paged response with correct metadata")
    void getAllStudents_returnsPaginatedResult() {
        Page<Student> page = new PageImpl<>(List.of(sampleStudent), PageRequest.of(0, 10), 1);
        when(studentRepository.findAllWithUserAccount(any(Pageable.class))).thenReturn(page);

        PageResponse<StudentResponse> result = studentService.getAllStudents(0, 10, "id", "asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
        assertThat(result.getPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("getAllStudents: should cap page size at 100")
    void getAllStudents_sizeAbove100_isCapped() {
        Page<Student> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(studentRepository.findAllWithUserAccount(argThat(p -> p.getPageSize() <= 100))).thenReturn(page);

        studentService.getAllStudents(0, 999, "id", "asc");

        // Verify the pageable passed to the repo has size <= 100
        verify(studentRepository).findAllWithUserAccount(argThat(p -> p.getPageSize() <= 100));
    }

    @Test
    @DisplayName("getAllStudents: descending sort creates descending pageable")
    void getAllStudents_descSort_createsDescPageable() {
        Page<Student> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(studentRepository.findAllWithUserAccount(argThat(p ->
                p.getSort().getOrderFor("fullName") != null &&
                p.getSort().getOrderFor("fullName").getDirection() == Sort.Direction.DESC
        ))).thenReturn(page);

        studentService.getAllStudents(0, 10, "fullName", "desc");

        verify(studentRepository).findAllWithUserAccount(argThat(p ->
                p.getSort().getOrderFor("fullName").getDirection() == Sort.Direction.DESC));
    }
}

