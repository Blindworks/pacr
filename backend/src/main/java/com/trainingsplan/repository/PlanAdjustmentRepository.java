package com.trainingsplan.repository;

import com.trainingsplan.entity.AdjustmentStatus;
import com.trainingsplan.entity.PlanAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PlanAdjustmentRepository extends JpaRepository<PlanAdjustment, Long> {

    List<PlanAdjustment> findByUserIdAndStatus(Long userId, AdjustmentStatus status);

    List<PlanAdjustment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PlanAdjustment> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime since);

    boolean existsByUserTrainingEntry_IdAndStatusIn(Long entryId, List<AdjustmentStatus> statuses);
}
