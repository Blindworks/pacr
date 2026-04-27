package com.trainingsplan.repository;

import com.trainingsplan.entity.LadvStagedEvent;
import com.trainingsplan.entity.LadvStagedEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LadvStagedEventRepository extends JpaRepository<LadvStagedEvent, Long> {

    Optional<LadvStagedEvent> findBySourceIdAndLadvId(Long sourceId, Long ladvId);

    @Query("""
            SELECT e FROM LadvStagedEvent e
            WHERE (:sourceId IS NULL OR e.source.id = :sourceId)
              AND (:status IS NULL OR e.status = :status)
              AND (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                OR LOWER(e.ort) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.startDate ASC, e.id ASC
            """)
    Page<LadvStagedEvent> search(@Param("sourceId") Long sourceId,
                                 @Param("status") LadvStagedEventStatus status,
                                 @Param("q") String q,
                                 Pageable pageable);
}
