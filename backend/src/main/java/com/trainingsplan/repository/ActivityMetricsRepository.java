package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityMetrics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityMetricsRepository extends JpaRepository<ActivityMetrics, Long> {

    Optional<ActivityMetrics> findByCompletedTrainingId(Long completedTrainingId);
    void deleteByCompletedTrainingId(Long completedTrainingId);

    List<ActivityMetrics> findByCompletedTrainingIdIn(List<Long> completedTrainingIds);

    /** Returns the most recent activities with eligible decoupling data for the given user. */
    @Query("SELECT am FROM ActivityMetrics am " +
           "JOIN FETCH am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND am.decouplingEligible = true " +
           "ORDER BY ct.trainingDate DESC, ct.id DESC")
    List<ActivityMetrics> findEligibleDecouplingByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT am FROM ActivityMetrics am " +
           "JOIN FETCH am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND am.decouplingEligible = true " +
           "AND ct.trainingDate >= :startDate AND ct.trainingDate <= :endDate " +
           "ORDER BY ct.trainingDate ASC, ct.id ASC")
    List<ActivityMetrics> findEligibleDecouplingByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(am.strain21) FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND ct.trainingDate = :date AND am.strain21 IS NOT NULL")
    Double sumStrain21ByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT SUM(am.trimp) FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND ct.trainingDate = :date AND am.trimp IS NOT NULL")
    Double sumTrimpByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT am FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId " +
           "AND am.efficiencyFactor IS NOT NULL " +
           "AND ct.trainingDate >= :startDate " +
           "AND ct.trainingDate <= :endDate")
    List<ActivityMetrics> findWithEfByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sums (z4Min + z5Min) for all activities of a user in a date range.
     * Returns 0.0 if no activities exist in that range.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(am.z4Min, 0.0) + COALESCE(am.z5Min, 0.0)), 0.0) " +
           "FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId " +
           "AND ct.trainingDate >= :startDate AND ct.trainingDate <= :endDate")
    double sumZ4Z5MinByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sums (z4Min + z5Min) per day for a date range.
     * Returns rows of {@code [LocalDate trainingDate, Double z4z5Sum]} — only days with
     * at least one activity are included. Callers must fill missing days with 0.0 if needed.
     */
    @Query("SELECT ct.trainingDate, COALESCE(SUM(COALESCE(am.z4Min, 0.0) + COALESCE(am.z5Min, 0.0)), 0.0) " +
           "FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId " +
           "AND ct.trainingDate >= :startDate AND ct.trainingDate <= :endDate " +
           "GROUP BY ct.trainingDate")
    List<Object[]> sumZ4Z5MinByUserIdAndDateRangeGrouped(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
