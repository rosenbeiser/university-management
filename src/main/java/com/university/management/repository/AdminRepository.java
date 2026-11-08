package com.university.management.repository;

import com.university.management.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Query("SELECT a FROM Admin a JOIN FETCH a.userAccount")
    Page<Admin> findAllWithUserAccount(Pageable pageable);

    @Query("SELECT a FROM Admin a JOIN FETCH a.userAccount WHERE a.id = :id")
    Optional<Admin> findByIdWithUserAccount(Long id);

    @Query("SELECT COUNT(a) FROM Admin a JOIN a.userAccount ua WHERE ua.enabled = true")
    long countActiveAdmins();
}
