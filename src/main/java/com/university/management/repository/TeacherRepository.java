package com.university.management.repository;

import com.university.management.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @Query("SELECT t FROM Teacher t JOIN FETCH t.userAccount")
    Page<Teacher> findAllWithUserAccount(Pageable pageable);

    @Query("SELECT t FROM Teacher t JOIN FETCH t.userAccount WHERE t.id = :id")
    Optional<Teacher> findByIdWithUserAccount(Long id);
}
