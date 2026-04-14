package com.trainingsplan.repository;

import com.trainingsplan.entity.TrainingStepBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingStepBlockRepository extends JpaRepository<TrainingStepBlock, Long> {
}
