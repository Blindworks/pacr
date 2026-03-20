package com.trainingsplan.repository;

import com.trainingsplan.entity.AppNewsSentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppNewsSentLogRepository extends JpaRepository<AppNewsSentLog, Long> {

    boolean existsByUserIdAndNewsId(Long userId, Long newsId);
}
