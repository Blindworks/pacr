package com.trainingsplan.repository;

import com.trainingsplan.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByKey(String key);

    List<Achievement> findAllByOrderBySortOrderAsc();
}
