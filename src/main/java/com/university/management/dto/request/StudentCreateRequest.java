package com.university.management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StudentCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, or underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Student number is required")
    @Size(min = 3, max = 30, message = "Student number must be between 3 and 30 characters")
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "Student number must contain only uppercase letters, digits, or hyphens")
    private String studentNumber;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester must not exceed 12")
    private Integer semester;

    @Pattern(regexp = "^[0-9+\\-\\s()]{0,20}$", message = "Phone number format is invalid")
    private String phone;
}
