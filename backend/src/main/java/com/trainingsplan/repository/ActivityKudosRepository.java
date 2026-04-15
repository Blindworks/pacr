package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityKudos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityKudosRepository extends JpaRepository<ActivityKudos, Long> {

    Optional<ActivityKudos> findByCompletedTraining_IdAndUser_Id(Long completedTrainingId, Long userId);

    boolean existsByCompletedTraining_IdAndUser_Id(Long completedTrainingId, Long userId);

    long countByCompletedTraining_Id(Long completedTrainingId);

    void deleteByCompletedTraining_IdAndUser_Id(Long completedTrainingId, Long userId);
}
