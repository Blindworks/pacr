package com.trainingsplan.repository;

import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompletedTrainingRepository extends JpaRepository<CompletedTraining, Long> {
    
    List<CompletedTraining> findByTrainingDateOrderByUploadDateDesc(LocalDate trainingDate);
    
    List<CompletedTraining> findByTrainingDateBetweenOrderByTrainingDate(LocalDate startDate, LocalDate endDate);
    
    Optional<CompletedTraining> findTopByTrainingDateOrderByUploadDateDesc(LocalDate trainingDate);
    
    List<CompletedTraining> findBySportOrderByTrainingDateDesc(String sport);

    long countByTrainingDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByStravaActivityId(Long stravaActivityId);

    Optional<CompletedTraining> findByCorosLabelId(String corosLabelId);
    boolean existsByCorosLabelId(String corosLabelId);

    Optional<CompletedTraining> findByStravaActivityId(Long stravaActivityId);
    List<CompletedTraining> findByUserIdAndSourceAndTrainingDateBetween(Long userId, String source, LocalDate startDate, LocalDate endDate);
    List<CompletedTraining> findByUserIsNullAndSourceAndTrainingDateBetween(String source, LocalDate startDate, LocalDate endDate);

    List<CompletedTraining> findByUserId(Long userId);

    @Query("SELECT c FROM CompletedTraining c WHERE c.user.id = :userId OR c.user IS NULL")
    List<CompletedTraining> findByUserIdOrUserIsNull(@Param("userId") Long userId);

    List<CompletedTraining> findByUserIdAndTrainingDateBetweenOrderByTrainingDate(Long userId, LocalDate startDate, LocalDate endDate);

    List<CompletedTraining> findByUserIdAndTrainingDateBetweenOrderByTrainingDateDescUploadDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    List<CompletedTraining> findByUserIdAndTrainingDateOrderByUploadDateDesc(Long userId, LocalDate trainingDate);

    Optional<CompletedTraining> findTopByUserIdAndTrainingDateOrderByUploadDateDesc(Long userId, LocalDate trainingDate);

    Optional<CompletedTraining> findTopByUserIdAndSportContainingIgnoreCaseOrderByTrainingDateDescUploadDateDesc(Long userId, String sport);

    Optional<CompletedTraining> findTopByUserIdOrderByTrainingDateDescUploadDateDesc(Long userId);

    Optional<CompletedTraining> findByIdAndUserId(Long id, Long userId);

    List<CompletedTraining> findByUserIdOrderByTrainingDateDescUploadDateDesc(Long userId, Pageable pageable);

    @Query("SELECT DISTINCT c.trainingType FROM CompletedTraining c WHERE c.user.id = :userId AND c.trainingType IS NOT NULL ORDER BY c.trainingType")
    List<String> findDistinctTrainingTypesByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c.sport FROM CompletedTraining c WHERE c.user.id = :userId AND c.sport IS NOT NULL ORDER BY c.sport")
    List<String> findDistinctSportsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.distanceKm), 0) FROM CompletedTraining c WHERE c.user.id = :userId")
    double sumDistanceKmByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.distanceKm), 0) FROM CompletedTraining c WHERE c.user.id = :userId AND c.trainingDate BETWEEN :from AND :to")
    double sumDistanceKmByUserIdAndDateBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT DISTINCT c.trainingDate FROM CompletedTraining c WHERE c.user.id = :userId ORDER BY c.trainingDate DESC")
    List<LocalDate> findDistinctTrainingDatesByUserIdOrderByTrainingDateDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c.trainingDate FROM CompletedTraining c WHERE c.user.id = :userId AND c.trainingDate BETWEEN :from AND :to ORDER BY c.trainingDate DESC")
    List<LocalDate> findDistinctTrainingDatesByUserIdAndDateBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(c) FROM CompletedTraining c WHERE c.user.id = :userId AND c.trainingDate = :date")
    long countByUserIdAndTrainingDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * One-time migration: assigns the current user to any Strava activities that were
     * synced before per-user tracking was added (user_id = NULL).
     */
    @Modifying
    @Query("UPDATE CompletedTraining ct SET ct.user = :user WHERE ct.user IS NULL AND ct.source = 'STRAVA'")
    int claimOrphanedStravaActivities(@Param("user") User user);
}
