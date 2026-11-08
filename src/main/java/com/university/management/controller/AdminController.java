package com.university.management.controller;

import com.university.management.dto.request.AdminCreateRequest;
import com.university.management.dto.request.AdminUpdateRequest;
import com.university.management.dto.response.AdminResponse;
import com.university.management.dto.response.PageResponse;
import com.university.management.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "CRUD operations for Admin users — ADMIN role required")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @PostMapping
    @Operation(summary = "Create Admin", description = "Create a new Admin account. ADMIN role required.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Admin created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "409", description = "Duplicate username or email")
    })
    public ResponseEntity<AdminResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        AdminResponse response = adminService.createAdmin(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List Admins", description = "Get paginated list of all Admins. ADMIN role required.")
    public ResponseEntity<PageResponse<AdminResponse>> getAllAdmins(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction")        @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.getAllAdmins(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Admin by ID", description = "Retrieve a single Admin by their ID. ADMIN role required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin found"),
        @ApiResponse(responseCode = "404", description = "Admin not found")
    })
    public ResponseEntity<AdminResponse> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAdminById(id));
    }

    @PutMapping("/{id}")
    @PatchMapping("/{id}")
    @Operation(summary = "Update Admin", description = "Update Admin fields (PUT/PATCH both supported). ADMIN role required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin updated"),
        @ApiResponse(responseCode = "404", description = "Admin not found"),
        @ApiResponse(responseCode = "409", description = "Email conflict")
    })
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Admin", description = "Delete an Admin account. Cannot delete the last active Admin. ADMIN role required.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Admin deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot delete last Admin"),
        @ApiResponse(responseCode = "404", description = "Admin not found")
    })
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
