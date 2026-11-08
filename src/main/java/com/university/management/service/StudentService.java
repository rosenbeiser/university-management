package com.university.management.service;

import com.university.management.dto.request.StudentCreateRequest;
import com.university.management.dto.request.StudentUpdateRequest;
import com.university.management.dto.response.PageResponse;
import com.university.management.dto.response.StudentResponse;
import com.university.management.dto.response.UserAccountResponse;
import com.university.management.entity.Student;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (studentRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new DuplicateResourceException("Student number already exists: " + request.getStudentNumber());
        }

        // Server always assigns STUDENT role
        UserAccount account = UserAccount.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .enabled(true)
                .build();
        account = userAccountRepository.save(account);

        Student student = Student.builder()
                .userAccount(account)
                .fullName(request.getFullName())
                .studentNumber(request.getStudentNumber())
                .department(request.getDepartment())
                .semester(request.getSemester())
                .phone(request.getPhone())
                .build();
        student = studentRepository.save(student);

        return mapToResponse(student);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getAllStudents(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<Student> studentPage = studentRepository.findAllWithUserAccount(pageable);

        return PageResponse.<StudentResponse>builder()
                .content(studentPage.getContent().stream().map(StudentService::mapToResponse).toList())
                .page(studentPage.getNumber())
                .size(studentPage.getSize())
                .totalElements(studentPage.getTotalElements())
                .totalPages(studentPage.getTotalPages())
                .last(studentPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        return mapToResponse(student);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Student student = studentRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        UserAccount account = student.getUserAccount();

        if (StringUtils.hasText(request.getFullName()))   student.setFullName(request.getFullName());
        if (StringUtils.hasText(request.getDepartment())) student.setDepartment(request.getDepartment());
        if (request.getSemester() != null)                student.setSemester(request.getSemester());
        if (request.getPhone() != null)                   student.setPhone(request.getPhone());

        if (StringUtils.hasText(request.getEmail())) {
            if (!request.getEmail().equalsIgnoreCase(account.getEmail())
                    && userAccountRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already in use: " + request.getEmail());
            }
            account.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPassword())) {
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEnabled() != null) {
            account.setEnabled(request.getEnabled());
        }

        userAccountRepository.save(account);
        studentRepository.save(student);
        return mapToResponse(student);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        userAccountRepository.delete(student.getUserAccount());
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    public static StudentResponse mapToResponse(Student student) {
        UserAccount ua = student.getUserAccount();
        return StudentResponse.builder()
                .id(student.getId())
                .fullName(student.getFullName())
                .studentNumber(student.getStudentNumber())
                .department(student.getDepartment())
                .semester(student.getSemester())
                .phone(student.getPhone())
                .account(UserAccountResponse.builder()
                        .id(ua.getId())
                        .username(ua.getUsername())
                        .email(ua.getEmail())
                        .role(ua.getRole())
                        .enabled(ua.isEnabled())
                        .createdAt(ua.getCreatedAt())
                        .updatedAt(ua.getUpdatedAt())
                        .build())
                .build();
    }
}
