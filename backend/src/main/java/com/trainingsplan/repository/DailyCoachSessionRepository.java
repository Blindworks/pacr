package com.trainingsplan.repository;

import com.trainingsplan.entity.DailyCoachSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCoachSessionRepository extends JpaRepository<DailyCoachSession, Long> {

    Optional<DailyCoachSession> findFirstByUserIdAndSessionDateOrderByIdDesc(Long userId, LocalDate date);

    List<DailyCoachSession> findByUserIdAndSessionDateBetween(Long userId, LocalDate from, LocalDate to);
}
