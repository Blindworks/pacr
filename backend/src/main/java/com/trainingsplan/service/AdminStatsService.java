package com.trainingsplan.service;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;

    public AdminStatsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AdminStatsDto getStats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        return new AdminStatsDto(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.INACTIVE),
                userRepository.countByStatus(UserStatus.BLOCKED),
                userRepository.countByStatusIn(List.of(
                        UserStatus.EMAIL_VERIFICATION_PENDING,
                        UserStatus.ADMIN_APPROVAL_PENDING)),
                userRepository.countByCreatedAtAfter(weekAgo),
                userRepository.countByCreatedAtAfter(monthAgo),
                userRepository.countByStravaTokenIsNotNull(),
                userRepository.countByAsthmaTrackingEnabledTrue(),
                userRepository.countByCycleTrackingEnabledTrue(),
                userRepository.countByThresholdPaceSecPerKmIsNotNull()
        );
    }
}
