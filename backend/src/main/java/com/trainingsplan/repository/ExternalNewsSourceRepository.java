package com.trainingsplan.repository;

import com.trainingsplan.entity.ExternalNewsSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalNewsSourceRepository extends JpaRepository<ExternalNewsSource, Long> {

    List<ExternalNewsSource> findAllByEnabledTrue();

    Optional<ExternalNewsSource> findByName(String name);
}
