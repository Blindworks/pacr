package com.trainingsplan.repository;

import com.trainingsplan.entity.CompetitionFormat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionFormatRepository extends JpaRepository<CompetitionFormat, Long> {
    List<CompetitionFormat> findByCompetitionId(Long competitionId);
}
