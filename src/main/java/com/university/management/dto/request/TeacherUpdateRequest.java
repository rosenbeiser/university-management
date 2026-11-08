package com.university.management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TeacherUpdateRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @Pattern(regexp = "^[0-9+\\-\\s()]{0,20}$", message = "Phone number format is invalid")
    private String phone;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    private Boolean enabled;
}
