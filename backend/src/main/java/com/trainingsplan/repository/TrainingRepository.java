package com.trainingsplan.repository;

import com.trainingsplan.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long> {

    List<Training> findByTrainingPlan_Id(Long trainingPlanId);

    @Query("SELECT t FROM Training t LEFT JOIN FETCH t.steps WHERE t.id = :id")
    Optional<Training> findByIdWithSteps(@Param("id") Long id);

    @Query("SELECT t FROM Training t LEFT JOIN FETCH t.prepTips WHERE t.id = :id")
    Optional<Training> findByIdWithPrepTips(@Param("id") Long id);
}
