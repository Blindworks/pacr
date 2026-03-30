package com.trainingsplan.repository;

import com.trainingsplan.entity.BodyMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, Long> {

    List<BodyMeasurement> findByUserIdOrderByMeasuredAtDesc(Long userId);

    Optional<BodyMeasurement> findTopByUserIdOrderByMeasuredAtDesc(Long userId);

    Optional<BodyMeasurement> findByIdAndUserId(Long id, Long userId);

    Optional<BodyMeasurement> findByExternalIdAndUserId(String externalId, Long userId);
}
