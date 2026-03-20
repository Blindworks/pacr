package com.trainingsplan.repository;

import com.trainingsplan.entity.PersonalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    List<PersonalRecord> findByUserIdOrderByDistanceKmAsc(Long userId);

    Optional<PersonalRecord> findByUserIdAndDistanceKm(Long userId, Double distanceKm);
}
