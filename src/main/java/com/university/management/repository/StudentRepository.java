package com.university.management.repository;

import com.university.management.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s JOIN FETCH s.userAccount")
    Page<Student> findAllWithUserAccount(Pageable pageable);

    @Query("SELECT s FROM Student s JOIN FETCH s.userAccount WHERE s.id = :id")
    Optional<Student> findByIdWithUserAccount(Long id);

    boolean existsByStudentNumber(String studentNumber);
}
