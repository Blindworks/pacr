package com.trainingsplan.service;

import com.trainingsplan.dto.CommunityRouteDetailDto;
import com.trainingsplan.dto.CommunityRouteDto;
import com.trainingsplan.dto.CreateCommunityRouteRequest;
import com.trainingsplan.dto.UpdateCommunityRouteRequest;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.ActivityStreamRepository;
import com.trainingsplan.repository.CommunityRouteRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.RouteAttemptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommunityRouteService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final CommunityRouteRepository communityRouteRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final RouteAttemptRepository routeAttemptRepository;
    private final ObjectMapper objectMapper;

    public CommunityRouteService(CommunityRouteRepository communityRouteRepository,
                                 CompletedTrainingRepository completedTrainingRepository,
                                 ActivityStreamRepository activityStreamRepository,
                                 RouteAttemptRepository routeAttemptRepository,
                                 ObjectMapper objectMapper) {
        this.communityRouteRepository = communityRouteRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.routeAttemptRepository = routeAttemptRepository;
        this.objectMapper = objectMapper;
    }

    public CommunityRouteDto shareRoute(User user, CreateCommunityRouteRequest request) {
        if (!user.isCommunityRoutesEnabled()) {
            throw new IllegalStateException("Community routes feature is not enabled for this user");
        }

        CompletedTraining activity = completedTrainingRepository.findById(request.activityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));

        if (!activity.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Activity does not belong to the user");
        }

        if (communityRouteRepository.existsBySourceActivityId(request.activityId())) {
            throw new IllegalArgumentException("Activity is already shared as a community route");
        }

        ActivityStream stream = activityStreamRepository.findByCompletedTrainingId(request.activityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity has no GPS data"));

        if (stream.getLatlngJson() == null || stream.getLatlngJson().isBlank()) {
            throw new IllegalArgumentException("Activity has no GPS data");
        }

        // Use start coordinates from activity, or extract from first GPS point in stream
        Double startLat = activity.getStartLatitude();
        Double startLng = activity.getStartLongitude();
        if (startLat == null || startLng == null) {
            double[] firstPoint = extractFirstGpsPoint(stream.getLatlngJson());
            if (firstPoint == null) {
                throw new IllegalArgumentException("Activity has no GPS data");
            }
            startLat = firstPoint[0];
            startLng = firstPoint[1];
        }

        CommunityRoute route = new CommunityRoute();
        route.setCreator(user);
        route.setSourceActivity(activity);
        route.setName(request.name());
        route.setDistanceKm(activity.getDistanceKm());
        route.setElevationGainM(activity.getElevationGainM());
        route.setStartLatitude(startLat);
        route.setStartLongitude(startLng);
        route.setGpsTrackJson(stream.getLatlngJson());
        route.setCreatedAt(LocalDateTime.now());

        if (request.visibility() != null && !request.visibility().isBlank()) {
            route.setVisibility(RouteVisibility.valueOf(request.visibility()));
        }

        CommunityRoute saved = communityRouteRepository.save(route);
        return toDto(saved, 0, null, null);
    }

    public void unshareRoute(User user, Long routeId) {
        CommunityRoute route = communityRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        if (!route.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Route does not belong to the user");
        }

        communityRouteRepository.delete(route);
    }

    public CommunityRouteDto updateRoute(User user, Long routeId, UpdateCommunityRouteRequest request) {
        CommunityRoute route = communityRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        if (!route.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Route does not belong to the user");
        }

        if (request.name() != null && !request.name().isBlank()) {
            route.setName(request.name());
        }
        if (request.visibility() != null && !request.visibility().isBlank()) {
            route.setVisibility(RouteVisibility.valueOf(request.visibility()));
        }
        route.setUpdatedAt(LocalDateTime.now());

        CommunityRoute saved = communityRouteRepository.save(route);
        return enrichDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommunityRouteDto> findNearbyRoutes(double lat, double lon, double radiusKm,
                                                     String sortBy, int page, int size) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<CommunityRoute> candidates = communityRouteRepository.findInBoundingBox(
                lat - latDelta, lat + latDelta,
                lon - lonDelta, lon + lonDelta);

        List<RouteWithDistance> filtered = new ArrayList<>();
        for (CommunityRoute route : candidates) {
            double dist = haversineDistance(lat, lon, route.getStartLatitude(), route.getStartLongitude());
            if (dist <= radiusKm) {
                filtered.add(new RouteWithDistance(route, dist));
            }
        }

        if ("popularity".equals(sortBy)) {
            filtered.sort((a, b) -> {
                long countA = routeAttemptRepository.countDistinctUsersByRouteIdAndStatus(a.route.getId(), AttemptStatus.COMPLETED);
                long countB = routeAttemptRepository.countDistinctUsersByRouteIdAndStatus(b.route.getId(), AttemptStatus.COMPLETED);
                return Long.compare(countB, countA);
            });
        } else {
            filtered.sort(Comparator.comparingDouble(a -> a.distance));
        }

        int fromIndex = page * size;
        if (fromIndex >= filtered.size()) return List.of();
        int toIndex = Math.min(fromIndex + size, filtered.size());

        return filtered.subList(fromIndex, toIndex).stream()
                .map(rd -> enrichDto(rd.route))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityRouteDetailDto getRouteDetail(Long routeId) {
        CommunityRoute route = communityRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        long athleteCount = routeAttemptRepository.countDistinctUsersByRouteIdAndStatus(routeId, AttemptStatus.COMPLETED);
        Optional<RouteAttempt> record = routeAttemptRepository.findFirstByRouteIdAndStatusOrderByTimeSecondsAsc(routeId, AttemptStatus.COMPLETED);

        double[][] gpsTrack = parseGpsTrack(route.getGpsTrackJson());

        return new CommunityRouteDetailDto(
                route.getId(),
                route.getName(),
                route.getDistanceKm(),
                route.getElevationGainM(),
                route.getStartLatitude(),
                route.getStartLongitude(),
                route.getCreator().getUsername(),
                route.getCreator().getId(),
                (int) athleteCount,
                record.map(RouteAttempt::getTimeSeconds).orElse(null),
                record.map(r -> r.getUser().getUsername()).orElse(null),
                route.getVisibility().name(),
                route.getCreatedAt(),
                gpsTrack
        );
    }

    @Transactional(readOnly = true)
    public List<CommunityRouteDto> getMyRoutes(User user) {
        return communityRouteRepository.findByCreatorId(user.getId()).stream()
                .map(this::enrichDto)
                .toList();
    }

    private CommunityRouteDto enrichDto(CommunityRoute route) {
        long athleteCount = routeAttemptRepository.countDistinctUsersByRouteIdAndStatus(route.getId(), AttemptStatus.COMPLETED);
        Optional<RouteAttempt> record = routeAttemptRepository.findFirstByRouteIdAndStatusOrderByTimeSecondsAsc(route.getId(), AttemptStatus.COMPLETED);

        return toDto(route, (int) athleteCount,
                record.map(RouteAttempt::getTimeSeconds).orElse(null),
                record.map(r -> r.getUser().getUsername()).orElse(null));
    }

    private CommunityRouteDto toDto(CommunityRoute route, int athleteCount,
                                     Integer recordTimeSeconds, String recordHolder) {
        return new CommunityRouteDto(
                route.getId(),
                route.getName(),
                route.getDistanceKm(),
                route.getElevationGainM(),
                route.getStartLatitude(),
                route.getStartLongitude(),
                route.getCreator().getUsername(),
                route.getCreator().getId(),
                athleteCount,
                recordTimeSeconds,
                recordHolder,
                route.getVisibility().name(),
                route.getCreatedAt()
        );
    }

    private double[] extractFirstGpsPoint(String latlngJson) {
        try {
            JsonNode root = objectMapper.readTree(latlngJson);
            JsonNode dataNode;
            if (root.isArray()) {
                dataNode = root;
            } else if (root.isObject() && root.has("data")) {
                dataNode = root.get("data");
            } else {
                return null;
            }
            for (JsonNode point : dataNode) {
                if (point.isArray() && point.size() >= 2 && !point.get(0).isNull() && !point.get(1).isNull()) {
                    double lat = point.get(0).doubleValue();
                    double lng = point.get(1).doubleValue();
                    if (lat != 0.0 || lng != 0.0) {
                        return new double[]{lat, lng};
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double[][] parseGpsTrack(String json) {
        if (json == null || json.isBlank()) {
            return new double[0][];
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode dataNode;
            if (root.isArray()) {
                dataNode = root;
            } else if (root.isObject() && root.has("data")) {
                dataNode = root.get("data");
            } else {
                return new double[0][];
            }
            int size = dataNode.size();
            double[][] result = new double[size][2];
            int idx = 0;
            for (JsonNode point : dataNode) {
                if (point.isArray() && point.size() >= 2 && !point.get(0).isNull() && !point.get(1).isNull()) {
                    result[idx][0] = point.get(0).doubleValue();
                    result[idx][1] = point.get(1).doubleValue();
                    idx++;
                }
            }
            if (idx < size) {
                double[][] trimmed = new double[idx][];
                System.arraycopy(result, 0, trimmed, 0, idx);
                return trimmed;
            }
            return result;
        } catch (Exception e) {
            return new double[0][];
        }
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private record RouteWithDistance(CommunityRoute route, double distance) {}
}
