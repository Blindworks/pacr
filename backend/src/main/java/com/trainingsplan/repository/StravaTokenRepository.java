package com.trainingsplan.repository;

import com.trainingsplan.entity.StravaToken;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StravaTokenRepository extends JpaRepository<StravaToken, Long> {
    Optional<StravaToken> findByUser(User user);
    void deleteByUser(User user);
}
