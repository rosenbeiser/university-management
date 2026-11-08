package com.university.management.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private String fullName;
    private String studentNumber;
    private String department;
    private Integer semester;
    private String phone;
    private UserAccountResponse account;
}
