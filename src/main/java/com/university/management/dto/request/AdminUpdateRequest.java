package com.university.management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminUpdateRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    // Password update (optional, omit to keep current)
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    // Account status — only admin can toggle
    private Boolean enabled;
}
