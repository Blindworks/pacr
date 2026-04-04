package com.trainingsplan.repository;

import com.trainingsplan.entity.GroupEvent;
import com.trainingsplan.entity.GroupEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {

    List<GroupEvent> findByTrainerIdOrderByEventDateDesc(Long trainerId);

    List<GroupEvent> findByStatusAndEventDateGreaterThanEqualOrderByEventDateAsc(GroupEventStatus status, LocalDate date);

    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status " +
           "AND ge.eventDate >= :date " +
           "AND ge.latitude BETWEEN :minLat AND :maxLat " +
           "AND ge.longitude BETWEEN :minLon AND :maxLon " +
           "ORDER BY ge.eventDate ASC")
    List<GroupEvent> findNearbyPublished(@Param("status") GroupEventStatus status,
                                        @Param("date") LocalDate date,
                                        @Param("minLat") double minLat,
                                        @Param("maxLat") double maxLat,
                                        @Param("minLon") double minLon,
                                        @Param("maxLon") double maxLon);

    // Recurring events: find all published recurring events whose series start is before range end
    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status AND ge.rrule IS NOT NULL " +
           "AND ge.eventDate <= :rangeEnd " +
           "AND (ge.recurrenceEndDate IS NULL OR ge.recurrenceEndDate >= :rangeStart)")
    List<GroupEvent> findPublishedRecurringInRange(@Param("status") GroupEventStatus status,
                                                   @Param("rangeStart") LocalDate rangeStart,
                                                   @Param("rangeEnd") LocalDate rangeEnd);

    // Nearby recurring events
    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status AND ge.rrule IS NOT NULL " +
           "AND ge.eventDate <= :rangeEnd " +
           "AND (ge.recurrenceEndDate IS NULL OR ge.recurrenceEndDate >= :rangeStart) " +
           "AND ge.latitude BETWEEN :minLat AND :maxLat " +
           "AND ge.longitude BETWEEN :minLon AND :maxLon")
    List<GroupEvent> findNearbyPublishedRecurringInRange(@Param("status") GroupEventStatus status,
                                                         @Param("rangeStart") LocalDate rangeStart,
                                                         @Param("rangeEnd") LocalDate rangeEnd,
                                                         @Param("minLat") double minLat,
                                                         @Param("maxLat") double maxLat,
                                                         @Param("minLon") double minLon,
                                                         @Param("maxLon") double maxLon);
}
