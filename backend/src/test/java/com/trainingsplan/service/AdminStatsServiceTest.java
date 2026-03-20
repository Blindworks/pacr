package com.trainingsplan.service;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AdminStatsServiceTest {

    private UserRepository userRepository;
    private AdminStatsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AdminStatsService(userRepository);
    }

    @Test
    void getStats_countsUsersCorrectly() {
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(5L);
        when(userRepository.countByStatus(UserStatus.INACTIVE)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.BLOCKED)).thenReturn(2L);
        when(userRepository.countByStatusIn(anyList())).thenReturn(3L);
        when(userRepository.count()).thenReturn(11L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(4L, 8L);
        when(userRepository.countByStravaTokenIsNotNull()).thenReturn(6L);
        when(userRepository.countByAsthmaTrackingEnabledTrue()).thenReturn(7L);
        when(userRepository.countByCycleTrackingEnabledTrue()).thenReturn(9L);
        when(userRepository.countByThresholdPaceSecPerKmIsNotNull()).thenReturn(10L);

        AdminStatsDto stats = service.getStats();

        assertEquals(11L, stats.totalUsers());
        assertEquals(5L, stats.activeUsers());
        assertEquals(1L, stats.inactiveUsers());
        assertEquals(2L, stats.blockedUsers());
        assertEquals(3L, stats.pendingVerification());
        assertEquals(4L, stats.newUsersThisWeek());
        assertEquals(8L, stats.newUsersThisMonth());
        assertEquals(6L, stats.stravaConnected());
        assertEquals(7L, stats.asthmaTrackingEnabled());
        assertEquals(9L, stats.cycleTrackingEnabled());
        assertEquals(10L, stats.paceZonesConfigured());
    }
}
