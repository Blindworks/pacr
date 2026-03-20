package com.trainingsplan.repository;

import com.trainingsplan.entity.PersonalRecordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRecordEntryRepository extends JpaRepository<PersonalRecordEntry, Long> {

    List<PersonalRecordEntry> findByPersonalRecordIdOrderByAchievedDateDesc(Long personalRecordId);

    Optional<PersonalRecordEntry> findTopByPersonalRecordIdOrderByTimeSecondsAsc(Long personalRecordId);

    boolean existsByPersonalRecordIdAndCompletedTrainingId(Long personalRecordId, Long completedTrainingId);

    @Query("SELECT COUNT(e) > 0 FROM PersonalRecordEntry e WHERE e.id = :entryId AND e.personalRecord.user.id = :userId")
    boolean existsByIdAndUserId(@Param("entryId") Long entryId, @Param("userId") Long userId);
}
