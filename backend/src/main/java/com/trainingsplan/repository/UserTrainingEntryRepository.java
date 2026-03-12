package com.trainingsplan.repository;

import com.trainingsplan.entity.UserTrainingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserTrainingEntryRepository extends JpaRepository<UserTrainingEntry, Long> {

    List<UserTrainingEntry> findByCompetitionRegistrationId(Long registrationId);

    List<UserTrainingEntry> findByCompetitionRegistration_Competition_Id(Long competitionId);

    List<UserTrainingEntry> findByCompetitionRegistrationIdAndWeekNumber(Long registrationId, Integer weekNumber);

    @Query("SELECT e FROM UserTrainingEntry e " +
           "LEFT JOIN FETCH e.competitionRegistration reg " +
           "LEFT JOIN FETCH reg.competition " +
           "LEFT JOIN FETCH e.training t " +
           "LEFT JOIN FETCH t.trainingPlan " +
           "WHERE reg.user.id = :userId " +
           "AND e.trainingDate BETWEEN :from AND :to")
    List<UserTrainingEntry> findCalendarEntriesForUser(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    void deleteByCompetitionRegistrationId(Long registrationId);

    @Query("SELECT MAX(t.weekNumber) FROM Training t WHERE t.trainingPlan.id = :planId")
    Integer findMaxWeekNumberByPlanId(@Param("planId") Long planId);
}
