package com.university.management.repository;

import com.university.management.entity.RefreshToken;
import com.university.management.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userAccount = :userAccount AND r.revoked = false")
    void revokeAllByUserAccount(UserAccount userAccount);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userAccount = :userAccount")
    void deleteAllByUserAccount(UserAccount userAccount);
}
