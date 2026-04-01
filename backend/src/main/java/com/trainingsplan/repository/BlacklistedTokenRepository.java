package com.trainingsplan.repository;

import com.trainingsplan.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiresAt < :now")
    int deleteExpiredTokens(LocalDateTime now);
}
