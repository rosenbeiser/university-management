package com.university.management.service;

import com.university.management.dto.request.AdminCreateRequest;
import com.university.management.dto.request.AdminUpdateRequest;
import com.university.management.dto.response.AdminResponse;
import com.university.management.dto.response.DashboardResponse;
import com.university.management.dto.response.PageResponse;
import com.university.management.dto.response.UserAccountResponse;
import com.university.management.entity.Admin;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.BusinessException;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public AdminResponse createAdmin(AdminCreateRequest request) {
        // Uniqueness checks
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Create account — role always forced to ADMIN on server
        UserAccount account = UserAccount.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        account = userAccountRepository.save(account);

        // Create profile
        Admin admin = Admin.builder()
                .userAccount(account)
                .fullName(request.getFullName())
                .department(request.getDepartment())
                .build();
        admin = adminRepository.save(admin);

        return mapToResponse(admin);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AdminResponse> getAllAdmins(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<Admin> adminPage = adminRepository.findAllWithUserAccount(pageable);

        return PageResponse.<AdminResponse>builder()
                .content(adminPage.getContent().stream().map(AdminService::mapToResponse).toList())
                .page(adminPage.getNumber())
                .size(adminPage.getSize())
                .totalElements(adminPage.getTotalElements())
                .totalPages(adminPage.getTotalPages())
                .last(adminPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminResponse getAdminById(Long id) {
        Admin admin = adminRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));
        return mapToResponse(admin);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public AdminResponse updateAdmin(Long id, AdminUpdateRequest request) {
        Admin admin = adminRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));
        UserAccount account = admin.getUserAccount();

        if (StringUtils.hasText(request.getFullName())) {
            admin.setFullName(request.getFullName());
        }
        if (request.getDepartment() != null) {
            admin.setDepartment(request.getDepartment());
        }
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
            // Prevent disabling the last active admin
            if (!request.getEnabled() && adminRepository.countActiveAdmins() <= 1) {
                throw new BusinessException("Cannot disable the last active Admin account.");
            }
            account.setEnabled(request.getEnabled());
        }

        userAccountRepository.save(account);
        adminRepository.save(admin);
        return mapToResponse(admin);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteAdmin(Long id) {
        Admin admin = adminRepository.findByIdWithUserAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));

        if (adminRepository.countActiveAdmins() <= 1 && admin.getUserAccount().isEnabled()) {
            throw new BusinessException("Cannot delete the last active Admin account.");
        }
        // Cascade delete: FK ON DELETE CASCADE removes the admin profile when UserAccount is deleted
        userAccountRepository.delete(admin.getUserAccount());
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long admins   = userAccountRepository.countByRole(Role.ADMIN);
        long teachers = userAccountRepository.countByRole(Role.TEACHER);
        long students = userAccountRepository.countByRole(Role.STUDENT);
        long activeAdmins = adminRepository.countActiveAdmins();

        return DashboardResponse.builder()
                .totalAdmins(admins)
                .totalTeachers(teachers)
                .totalStudents(students)
                .totalUsers(admins + teachers + students)
                .activeAdmins(activeAdmins)
                .build();
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    public static AdminResponse mapToResponse(Admin admin) {
        UserAccount ua = admin.getUserAccount();
        return AdminResponse.builder()
                .id(admin.getId())
                .fullName(admin.getFullName())
                .department(admin.getDepartment())
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
