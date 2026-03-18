package com.trainingsplan.repository;

import com.trainingsplan.entity.AsthmaEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AsthmaEntryRepository extends JpaRepository<AsthmaEntry, Long> {
    List<AsthmaEntry> findByUserIdOrderByLoggedAtDesc(Long userId);
    Optional<AsthmaEntry> findByIdAndUserId(Long id, Long userId);
    List<AsthmaEntry> findByUserIdAndLoggedAtAfterOrderByLoggedAtDesc(Long userId, LocalDateTime after);
}
