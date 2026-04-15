package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityCommentRepository extends JpaRepository<ActivityComment, Long> {

    List<ActivityComment> findByCompletedTraining_IdOrderByCreatedAtAsc(Long completedTrainingId);

    long countByCompletedTraining_Id(Long completedTrainingId);
}
