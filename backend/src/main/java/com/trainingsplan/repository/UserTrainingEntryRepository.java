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
           "LEFT JOIN FETCH reg.user " +
           "LEFT JOIN FETCH e.training t " +
           "LEFT JOIN FETCH t.trainingPlan " +
           "WHERE reg.user.id = :userId " +
           "AND e.trainingDate BETWEEN :from AND :to")
    List<UserTrainingEntry> findCalendarEntriesForUser(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT e FROM UserTrainingEntry e " +
           "LEFT JOIN FETCH e.competitionRegistration reg " +
           "LEFT JOIN FETCH reg.competition " +
           "LEFT JOIN FETCH reg.user " +
           "LEFT JOIN FETCH e.training t " +
           "LEFT JOIN FETCH t.trainingPlan " +
           "WHERE reg.user.id = :userId " +
           "AND e.trainingDate = :trainingDate")
    List<UserTrainingEntry> findEntriesForUserByDate(
            @Param("userId") Long userId,
            @Param("trainingDate") LocalDate trainingDate);

    void deleteByCompetitionRegistrationId(Long registrationId);

    void deleteByCompetitionRegistration_Competition_Id(Long competitionId);

    void deleteByTraining_TrainingPlan_Id(Long trainingPlanId);

    @Query("SELECT MAX(t.weekNumber) FROM Training t WHERE t.trainingPlan.id = :planId")
    Integer findMaxWeekNumberByPlanId(@Param("planId") Long planId);

    @Query("SELECT e FROM UserTrainingEntry e " +
           "LEFT JOIN FETCH e.competitionRegistration reg " +
           "LEFT JOIN FETCH reg.user " +
           "LEFT JOIN FETCH e.training t " +
           "WHERE reg.user.id = :userId " +
           "AND e.trainingDate < :today " +
           "AND e.trainingDate >= :since " +
           "AND e.completed = false " +
           "AND (e.completionStatus IS NULL OR e.completionStatus = '')")
    List<UserTrainingEntry> findMissedWorkouts(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("since") LocalDate since);

    @Query("SELECT e FROM UserTrainingEntry e " +
           "LEFT JOIN FETCH e.competitionRegistration reg " +
           "LEFT JOIN FETCH reg.user " +
           "LEFT JOIN FETCH e.training t " +
           "WHERE reg.user.id = :userId " +
           "AND e.trainingDate BETWEEN :from AND :to")
    List<UserTrainingEntry> findUpcomingEntries(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
