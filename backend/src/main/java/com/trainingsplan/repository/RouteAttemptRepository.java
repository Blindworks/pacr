package com.trainingsplan.repository;

import com.trainingsplan.entity.AttemptStatus;
import com.trainingsplan.entity.RouteAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RouteAttemptRepository extends JpaRepository<RouteAttempt, Long> {

    Optional<RouteAttempt> findByUserIdAndStatus(Long userId, AttemptStatus status);

    List<RouteAttempt> findByRouteIdAndStatusOrderByTimeSecondsAsc(Long routeId, AttemptStatus status);

    @Query("SELECT COUNT(DISTINCT ra.user.id) FROM RouteAttempt ra WHERE ra.route.id = :routeId AND ra.status = :status")
    long countDistinctUsersByRouteIdAndStatus(@Param("routeId") Long routeId, @Param("status") AttemptStatus status);

    Optional<RouteAttempt> findFirstByRouteIdAndStatusOrderByTimeSecondsAsc(Long routeId, AttemptStatus status);

    @Query("SELECT ra FROM RouteAttempt ra WHERE ra.route.id = :routeId AND ra.status = com.trainingsplan.entity.AttemptStatus.COMPLETED " +
           "AND ra.completedAt >= :since ORDER BY ra.timeSeconds ASC")
    List<RouteAttempt> findLeaderboard(@Param("routeId") Long routeId, @Param("since") LocalDateTime since);

    @Query("SELECT ra FROM RouteAttempt ra WHERE ra.route.id = :routeId AND ra.status = com.trainingsplan.entity.AttemptStatus.COMPLETED " +
           "ORDER BY ra.timeSeconds ASC")
    List<RouteAttempt> findAllTimeLeaderboard(@Param("routeId") Long routeId);

    List<RouteAttempt> findByUserIdAndStatusOrderByCompletedAtDesc(Long userId, AttemptStatus status);

    List<RouteAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);
}
