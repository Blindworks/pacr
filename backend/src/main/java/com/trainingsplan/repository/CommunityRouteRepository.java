package com.trainingsplan.repository;

import com.trainingsplan.entity.CommunityRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityRouteRepository extends JpaRepository<CommunityRoute, Long> {

    Optional<CommunityRoute> findBySourceActivityId(Long activityId);

    List<CommunityRoute> findByCreatorId(Long userId);

    boolean existsBySourceActivityId(Long activityId);

    @Query("SELECT cr FROM CommunityRoute cr WHERE cr.visibility = com.trainingsplan.entity.RouteVisibility.PUBLIC " +
           "AND cr.startLatitude BETWEEN :minLat AND :maxLat " +
           "AND cr.startLongitude BETWEEN :minLon AND :maxLon")
    List<CommunityRoute> findInBoundingBox(@Param("minLat") double minLat,
                                           @Param("maxLat") double maxLat,
                                           @Param("minLon") double minLon,
                                           @Param("maxLon") double maxLon);
}
