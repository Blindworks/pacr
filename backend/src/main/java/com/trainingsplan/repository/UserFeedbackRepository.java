package com.trainingsplan.repository;

import com.trainingsplan.entity.FeedbackStatus;
import com.trainingsplan.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    List<UserFeedback> findAllByOrderByCreatedAtDesc();
    List<UserFeedback> findAllByStatusOrderByCreatedAtDesc(FeedbackStatus status);
}
