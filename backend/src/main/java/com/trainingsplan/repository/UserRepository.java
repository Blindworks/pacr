package com.trainingsplan.repository;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT u FROM User u WHERE u.discoverableByOthers = true " +
            "AND u.status = com.trainingsplan.entity.UserStatus.ACTIVE " +
            "AND u.id <> :excludeUserId " +
            "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "  OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "  OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY u.username ASC")
    List<User> searchDiscoverableUsers(@Param("query") String query,
                                        @Param("excludeUserId") Long excludeUserId,
                                        Pageable pageable);
}
