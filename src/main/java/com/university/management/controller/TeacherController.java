package com.university.management.controller;

import com.university.management.dto.request.TeacherCreateRequest;
import com.university.management.dto.request.TeacherUpdateRequest;
import com.university.management.dto.response.PageResponse;
import com.university.management.dto.response.TeacherResponse;
import com.university.management.service.TeacherService;
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
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Management", description = "CRUD operations for Teacher resources. Only ADMIN can appoint, update, or delete faculty.")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Teacher", description = "Appoint a new Teacher/Faculty account. ONLY ADMIN can appoint faculty.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Teacher appointed successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
        @ApiResponse(responseCode = "409", description = "Duplicate username or email")
    })
    public ResponseEntity<TeacherResponse> createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
        TeacherResponse response = teacherService.createTeacher(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "List Teachers", description = "Get paginated list of all Teachers/Faculty members.")
    public ResponseEntity<PageResponse<TeacherResponse>> getAllTeachers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction")        @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(teacherService.getAllTeachers(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "Get Teacher by ID", description = "Retrieve a single Teacher by ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Teacher found"),
        @ApiResponse(responseCode = "404", description = "Teacher not found")
    })
    public ResponseEntity<TeacherResponse> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @PutMapping("/{id}")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Teacher", description = "Update Teacher fields (PUT/PATCH both supported). ONLY ADMIN can update faculty.")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherUpdateRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Teacher", description = "Delete a Teacher account. ONLY ADMIN can delete faculty.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Teacher deleted"),
        @ApiResponse(responseCode = "404", description = "Teacher not found")
    })
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
