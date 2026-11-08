package com.university.management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "student_number", nullable = false, unique = true, length = 30)
    private String studentNumber;

    @Column(nullable = false, length = 100)
    private String department;

    private Integer semester;

    @Column(length = 20)
    private String phone;
}
