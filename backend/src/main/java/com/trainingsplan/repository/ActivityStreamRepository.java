package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ActivityStreamRepository extends JpaRepository<ActivityStream, Long> {

    Optional<ActivityStream> findByCompletedTrainingId(Long completedTrainingId);

    @Modifying
    @Transactional
    void deleteByCompletedTrainingId(Long completedTrainingId);
}
