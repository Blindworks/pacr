package com.trainingsplan.repository;

import com.trainingsplan.entity.CompetitionRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRegistrationRepository extends JpaRepository<CompetitionRegistration, Long> {
    Optional<CompetitionRegistration> findByCompetitionIdAndUserId(Long competitionId, Long userId);
    List<CompetitionRegistration> findByUserId(Long userId);
    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    @Modifying
    @Query("UPDATE CompetitionRegistration r SET r.trainingPlan = NULL WHERE r.trainingPlan.id = :planId")
    void nullifyTrainingPlan(@Param("planId") Long planId);
}
