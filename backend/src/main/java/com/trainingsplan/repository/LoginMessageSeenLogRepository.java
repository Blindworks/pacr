package com.trainingsplan.repository;

import com.trainingsplan.entity.LoginMessageSeenLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginMessageSeenLogRepository extends JpaRepository<LoginMessageSeenLog, Long> {

    boolean existsByUserIdAndLoginMessageId(Long userId, Long loginMessageId);

    List<LoginMessageSeenLog> findAllByUserId(Long userId);
}
