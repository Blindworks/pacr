package com.trainingsplan.service;

import com.trainingsplan.dto.LeaderboardEntryDto;
import com.trainingsplan.entity.AttemptStatus;
import com.trainingsplan.entity.RouteAttempt;
import com.trainingsplan.repository.RouteAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

    private final RouteAttemptRepository routeAttemptRepository;

    public LeaderboardService(RouteAttemptRepository routeAttemptRepository) {
        this.routeAttemptRepository = routeAttemptRepository;
    }

    public List<LeaderboardEntryDto> getLeaderboard(Long routeId, String period) {
        List<RouteAttempt> attempts;
        if ("THIS_WEEK".equals(period)) {
            LocalDateTime startOfWeek = LocalDateTime.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .with(LocalTime.MIN);
            attempts = routeAttemptRepository.findLeaderboard(routeId, startOfWeek);
        } else if ("THIS_MONTH".equals(period)) {
            LocalDateTime startOfMonth = LocalDateTime.now()
                    .withDayOfMonth(1)
                    .with(LocalTime.MIN);
            attempts = routeAttemptRepository.findLeaderboard(routeId, startOfMonth);
        } else {
            attempts = routeAttemptRepository.findAllTimeLeaderboard(routeId);
        }
        return toLeaderboardEntries(attempts);
    }

    public int getPosition(Long routeId, int timeSeconds) {
        List<RouteAttempt> allTime = routeAttemptRepository.findAllTimeLeaderboard(routeId);
        int position = 1;
        for (RouteAttempt a : allTime) {
            if (a.getTimeSeconds() != null && a.getTimeSeconds() < timeSeconds) {
                position++;
            } else {
                break;
            }
        }
        return position;
    }

    private List<LeaderboardEntryDto> toLeaderboardEntries(List<RouteAttempt> attempts) {
        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int rank = 0;
        for (RouteAttempt a : attempts) {
            rank++;
            entries.add(new LeaderboardEntryDto(
                    rank,
                    a.getUser().getUsername(),
                    a.getUser().getId(),
                    a.getTimeSeconds(),
                    a.getPaceSecondsPerKm(),
                    a.getCompletedAt() != null ? a.getCompletedAt().toLocalDate() : null
            ));
        }
        return entries;
    }
}
