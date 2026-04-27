package com.trainingsplan.repository;

import com.trainingsplan.entity.LadvImportSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LadvImportSourceRepository extends JpaRepository<LadvImportSource, Long> {
    List<LadvImportSource> findAllByEnabledTrue();
    Optional<LadvImportSource> findByName(String name);
}
