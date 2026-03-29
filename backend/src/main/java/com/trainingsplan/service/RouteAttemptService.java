package com.trainingsplan.service;

import com.trainingsplan.dto.RouteAttemptDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.event.TrainingCompletedEvent;
import com.trainingsplan.repository.CommunityRouteRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.RouteAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RouteAttemptService {

    private static final Logger log = LoggerFactory.getLogger(RouteAttemptService.class);
    private static final double DISTANCE_TOLERANCE = 0.30;

    private final RouteAttemptRepository routeAttemptRepository;
    private final CommunityRouteRepository communityRouteRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final LeaderboardService leaderboardService;

    public RouteAttemptService(RouteAttemptRepository routeAttemptRepository,
                               CommunityRouteRepository communityRouteRepository,
                               CompletedTrainingRepository completedTrainingRepository,
                               LeaderboardService leaderboardService) {
        this.routeAttemptRepository = routeAttemptRepository;
        this.communityRouteRepository = communityRouteRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.leaderboardService = leaderboardService;
    }

    public RouteAttemptDto selectRoute(User user, Long routeId) {
        if (!user.isCommunityRoutesEnabled()) {
            throw new IllegalStateException("Community routes feature is not enabled");
        }

        Optional<RouteAttempt> existing = routeAttemptRepository.findByUserIdAndStatus(user.getId(), AttemptStatus.PENDING);
        if (existing.isPresent()) {
            throw new IllegalStateException("User already has a pending route attempt. Cancel it first.");
        }

        CommunityRoute route = communityRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        RouteAttempt attempt = new RouteAttempt();
        attempt.setRoute(route);
        attempt.setUser(user);
        attempt.setStatus(AttemptStatus.PENDING);
        attempt.setCreatedAt(LocalDateTime.now());

        RouteAttempt saved = routeAttemptRepository.save(attempt);
        return toDto(saved, null);
    }

    public void cancelPendingAttempt(User user) {
        RouteAttempt attempt = routeAttemptRepository.findByUserIdAndStatus(user.getId(), AttemptStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("No pending attempt found"));
        routeAttemptRepository.delete(attempt);
    }

    @Transactional(readOnly = true)
    public RouteAttemptDto getPendingAttempt(User user) {
        return routeAttemptRepository.findByUserIdAndStatus(user.getId(), AttemptStatus.PENDING)
                .map(a -> toDto(a, null))
                .orElse(null);
    }

    public RouteAttemptDto assignActivityToAttempt(User user, Long activityId) {
        Optional<RouteAttempt> pendingOpt = routeAttemptRepository.findByUserIdAndStatus(user.getId(), AttemptStatus.PENDING);
        if (pendingOpt.isEmpty()) {
            return null;
        }

        RouteAttempt attempt = pendingOpt.get();
        CompletedTraining activity = completedTrainingRepository.findById(activityId)
                .orElse(null);
        if (activity == null) {
            return null;
        }

        CommunityRoute route = attempt.getRoute();
        attempt.setActivity(activity);
        attempt.setCompletedAt(LocalDateTime.now());

        if (activity.getDistanceKm() != null && route.getDistanceKm() != null) {
            double diff = Math.abs(activity.getDistanceKm() - route.getDistanceKm()) / route.getDistanceKm();
            if (diff > DISTANCE_TOLERANCE) {
                attempt.setStatus(AttemptStatus.INVALID);
                routeAttemptRepository.save(attempt);
                return toDto(attempt, null);
            }
        }

        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setTimeSeconds(activity.getDurationSeconds());
        attempt.setPaceSecondsPerKm(activity.getAveragePaceSecondsPerKm());

        RouteAttempt saved = routeAttemptRepository.save(attempt);

        int position = leaderboardService.getPosition(route.getId(), saved.getTimeSeconds());
        return toDto(saved, position);
    }

    @Transactional(readOnly = true)
    public RouteAttemptDto getAttemptResult(Long attemptId, User user) {
        RouteAttempt attempt = routeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Attempt does not belong to user");
        }

        Integer position = null;
        if (attempt.getStatus() == AttemptStatus.COMPLETED && attempt.getTimeSeconds() != null) {
            position = leaderboardService.getPosition(attempt.getRoute().getId(), attempt.getTimeSeconds());
        }
        return toDto(attempt, position);
    }

    @Transactional(readOnly = true)
    public List<RouteAttemptDto> getMyAttempts(User user) {
        return routeAttemptRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(a -> {
                    Integer pos = null;
                    if (a.getStatus() == AttemptStatus.COMPLETED && a.getTimeSeconds() != null) {
                        pos = leaderboardService.getPosition(a.getRoute().getId(), a.getTimeSeconds());
                    }
                    return toDto(a, pos);
                })
                .toList();
    }

    @EventListener
    public void onTrainingCompleted(TrainingCompletedEvent event) {
        try {
            User user = event.getUser();
            if (user != null && user.isCommunityRoutesEnabled()) {
                assignActivityToAttempt(user, event.getCompletedTraining().getId());
            }
        } catch (Exception e) {
            log.warn("Could not assign activity to route attempt: {}", e.getMessage());
        }
    }

    private RouteAttemptDto toDto(RouteAttempt attempt, Integer leaderboardPosition) {
        return new RouteAttemptDto(
                attempt.getId(),
                attempt.getRoute().getId(),
                attempt.getRoute().getName(),
                attempt.getStatus().name(),
                attempt.getTimeSeconds(),
                attempt.getPaceSecondsPerKm(),
                attempt.getCompletedAt(),
                leaderboardPosition
        );
    }
}
