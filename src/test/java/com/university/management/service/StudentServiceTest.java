package com.university.management.service;

import com.university.management.dto.request.StudentCreateRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

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

    @Test
    @DisplayName("createStudent: should assign STUDENT role and validate studentNumber uniqueness")
    void createStudent_success() {
        StudentCreateRequest request = new StudentCreateRequest();
        request.setUsername("student01");
        request.setEmail("student01@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Jordan Lee");
        request.setStudentNumber("STU-2026-001");
        request.setDepartment("Computer Science");
        request.setSemester(1);

        when(userAccountRepository.existsByUsername("student01")).thenReturn(false);
        when(userAccountRepository.existsByEmail("student01@example.com")).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU-2026-001")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(studentRepository.save(any())).thenReturn(sampleStudent);

        StudentResponse response = studentService.createStudent(request);

        assertThat(response.getAccount().getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.getStudentNumber()).isEqualTo("STU-2026-001");
        verify(userAccountRepository).save(argThat(ua -> ua.getRole() == Role.STUDENT));
    }

    @Test
    @DisplayName("createStudent: should throw DuplicateResourceException for duplicate studentNumber")
    void createStudent_duplicateStudentNumber() {
        StudentCreateRequest request = new StudentCreateRequest();
        request.setUsername("student02");
        request.setEmail("student02@example.com");
        request.setPassword("pass");
        request.setFullName("Name");
        request.setStudentNumber("STU-2026-001");
        request.setDepartment("CS");

        when(userAccountRepository.existsByUsername("student02")).thenReturn(false);
        when(userAccountRepository.existsByEmail("student02@example.com")).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU-2026-001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("STU-2026-001");
    }

    @Test
    @DisplayName("getStudentById: should throw ResourceNotFoundException when not found")
    void getStudentById_notFound() {
        when(studentRepository.findByIdWithUserAccount(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteStudent: should delete via userAccount cascade")
    void deleteStudent_success() {
        when(studentRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleStudent));

        studentService.deleteStudent(1L);

        verify(userAccountRepository).delete(sampleAccount);
    }
}
