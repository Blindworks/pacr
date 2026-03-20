package com.trainingsplan.repository;

import com.trainingsplan.entity.AppNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNewsRepository extends JpaRepository<AppNews, Long> {

    List<AppNews> findAllByOrderByCreatedAtDesc();

    List<AppNews> findAllByIsPublishedTrueOrderByPublishedAtDesc();
}
