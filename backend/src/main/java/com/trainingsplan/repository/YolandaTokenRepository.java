package com.trainingsplan.repository;

import com.trainingsplan.entity.YolandaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YolandaTokenRepository extends JpaRepository<YolandaToken, Long> {
    Optional<YolandaToken> findByAccountId(String accountId);
    Optional<YolandaToken> findByUserId(Long userId);
}
