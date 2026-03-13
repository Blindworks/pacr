package com.trainingsplan.repository;

import com.trainingsplan.entity.CycleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CycleEntryRepository extends JpaRepository<CycleEntry, Long> {

    List<CycleEntry> findByUserIdOrderByEntryDateDesc(Long userId);

    Optional<CycleEntry> findTopByUserIdOrderByEntryDateDesc(Long userId);

    Optional<CycleEntry> findByIdAndUserId(Long id, Long userId);

    Optional<CycleEntry> findByUserIdAndEntryDate(Long userId, LocalDate date);

    boolean existsByUserIdAndEntryDateGreaterThanEqualAndFlowIntensityIsNotNull(Long userId, LocalDate date);
}
