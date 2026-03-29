package com.trainingsplan.repository;

import com.trainingsplan.entity.CorosToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorosTokenRepository extends JpaRepository<CorosToken, Long> {
    Optional<CorosToken> findByOpenId(String openId);
    Optional<CorosToken> findByUserId(Long userId);
}
