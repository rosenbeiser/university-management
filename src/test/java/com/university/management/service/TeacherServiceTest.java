package com.university.management.service;

import com.university.management.dto.request.TeacherCreateRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherService Unit Tests")
class TeacherServiceTest {

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

    @Test
    @DisplayName("createTeacher: should assign TEACHER role regardless of input")
    void createTeacher_assignsTeacherRole() {
        TeacherCreateRequest request = new TeacherCreateRequest();
        request.setUsername("teacher01");
        request.setEmail("teacher01@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Alex Morgan");
        request.setDepartment("Computer Science");
        request.setDesignation("Lecturer");
        request.setPhone("01700000000");

        when(userAccountRepository.existsByUsername("teacher01")).thenReturn(false);
        when(userAccountRepository.existsByEmail("teacher01@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(userAccountRepository.save(any())).thenReturn(sampleAccount);
        when(teacherRepository.save(any())).thenReturn(sampleTeacher);

        TeacherResponse response = teacherService.createTeacher(request);

        assertThat(response.getAccount().getRole()).isEqualTo(Role.TEACHER);
        verify(userAccountRepository).save(argThat(ua -> ua.getRole() == Role.TEACHER));
    }

    @Test
    @DisplayName("createTeacher: should throw when username is duplicate")
    void createTeacher_duplicateUsername() {
        TeacherCreateRequest request = new TeacherCreateRequest();
        request.setUsername("teacher01");
        request.setEmail("new@example.com");
        request.setPassword("pass");
        request.setFullName("Name");
        request.setDepartment("CS");

        when(userAccountRepository.existsByUsername("teacher01")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.createTeacher(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("getTeacherById: should throw ResourceNotFoundException when not found")
    void getTeacherById_notFound() {
        when(teacherRepository.findByIdWithUserAccount(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeacherById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTeacher: should delete via userAccount cascade")
    void deleteTeacher_success() {
        when(teacherRepository.findByIdWithUserAccount(1L)).thenReturn(Optional.of(sampleTeacher));

        teacherService.deleteTeacher(1L);

        verify(userAccountRepository).delete(sampleAccount);
    }
}
