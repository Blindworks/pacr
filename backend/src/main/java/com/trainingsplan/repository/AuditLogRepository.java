package com.trainingsplan.repository;

import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action IS NULL OR a.action = :action)
          AND (:from IS NULL OR a.timestamp >= :from)
          AND (:to IS NULL OR a.timestamp <= :to)
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findFiltered(
            @Param("action") AuditAction action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
