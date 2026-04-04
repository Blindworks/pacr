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
}
