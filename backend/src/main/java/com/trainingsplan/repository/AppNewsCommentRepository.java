package com.trainingsplan.repository;

import com.trainingsplan.entity.AppNewsComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppNewsCommentRepository extends JpaRepository<AppNewsComment, Long> {

    List<AppNewsComment> findByAppNews_IdOrderByCreatedAtAsc(Long newsId);

    long countByAppNews_Id(Long newsId);

    long countByAppNews_IdAndCreatedAtAfter(Long newsId, LocalDateTime after);
}
