package com.trainingsplan.repository;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByRole(UserRole role);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    long countByStatus(UserStatus status);

    long countByStatusIn(List<UserStatus> statuses);

    long countByCreatedAtAfter(LocalDateTime date);

    long countByStravaTokenIsNotNull();

    long countByAsthmaTrackingEnabledTrue();

    long countByCycleTrackingEnabledTrue();

    long countByThresholdPaceSecPerKmIsNotNull();
}
