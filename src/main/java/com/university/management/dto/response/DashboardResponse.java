package com.university.management.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {
    private long totalAdmins;
    private long totalTeachers;
    private long totalStudents;
    private long totalUsers;
    private long activeAdmins;
}
