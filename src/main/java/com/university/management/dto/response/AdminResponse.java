package com.university.management.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminResponse {
    private Long id;
    private String fullName;
    private String department;
    private UserAccountResponse account;
}
