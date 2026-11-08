package com.university.management.service;

import com.university.management.dto.request.TeacherCreateRequest;
import com.university.management.dto.request.TeacherUpdateRequest;
import com.university.management.dto.response.PageResponse;
import com.university.management.dto.response.TeacherResponse;
import com.university.management.dto.response.UserAccountResponse;
import com.university.management.entity.Teacher;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.TeacherRepository;
import com.university.management.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public TeacherResponse createTeacher(TeacherCreateRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Server always assigns TEACHER role
        UserAccount account = UserAccount.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.TEACHER)
                .enabled(true)
                .build();
        account = userAccountRepository.save(account);

        Teacher teacher = Teacher.builder()
                .userAccount(account)
                .fullName(request.getFullName())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .phone(request.getPhone())
                .build();
        teacher = teacherRepository.save(teacher);

        return mapToResponse(teacher);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> getAllTeachers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<Teacher> teacherPage = teacherRepository.findAllWithUserAccount(pageable);

        return PageResponse.<TeacherResponse>builder()
                .content(teacherPage.getContent().stream().map(TeacherService::mapToResponse).toList())
                .page(teacherPage.getNumber())
                .size(teacherPage.getSize())
                .totalElements(teacherPage.getTotalElements())
                .totalPages(teacherPage.getTotalPages())
                .last(teacherPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
        return mapToResponse(teacher);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public TeacherResponse updateTeacher(Long id, TeacherUpdateRequest request) {
        Teacher teacher = teacherRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
        UserAccount account = teacher.getUserAccount();

        if (StringUtils.hasText(request.getFullName()))   teacher.setFullName(request.getFullName());
        if (StringUtils.hasText(request.getDepartment())) teacher.setDepartment(request.getDepartment());
        if (request.getDesignation() != null)             teacher.setDesignation(request.getDesignation());
        if (request.getPhone() != null)                   teacher.setPhone(request.getPhone());

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
        teacherRepository.save(teacher);
        return mapToResponse(teacher);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
        userAccountRepository.delete(teacher.getUserAccount());
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    public static TeacherResponse mapToResponse(Teacher teacher) {
        UserAccount ua = teacher.getUserAccount();
        return TeacherResponse.builder()
                .id(teacher.getId())
                .fullName(teacher.getFullName())
                .department(teacher.getDepartment())
                .designation(teacher.getDesignation())
                .phone(teacher.getPhone())
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
