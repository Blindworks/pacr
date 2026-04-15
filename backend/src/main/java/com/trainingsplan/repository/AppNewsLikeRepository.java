package com.trainingsplan.repository;

import com.trainingsplan.entity.AppNewsLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AppNewsLikeRepository extends JpaRepository<AppNewsLike, Long> {

    boolean existsByAppNews_IdAndUser_Id(Long newsId, Long userId);

    long countByAppNews_Id(Long newsId);

    Optional<AppNewsLike> findByAppNews_IdAndUser_Id(Long newsId, Long userId);

    long countByAppNews_IdAndCreatedAtAfter(Long newsId, LocalDateTime after);
}
