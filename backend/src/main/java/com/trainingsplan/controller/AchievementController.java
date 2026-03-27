package com.trainingsplan.controller;

import com.trainingsplan.dto.AchievementDto;
import com.trainingsplan.entity.Achievement;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserAchievement;
import com.trainingsplan.repository.AchievementRepository;
import com.trainingsplan.repository.UserAchievementRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.AchievementEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementEvaluationService achievementEvaluationService;
    private final SecurityUtils securityUtils;

    public AchievementController(AchievementRepository achievementRepository,
                                  UserAchievementRepository userAchievementRepository,
                                  AchievementEvaluationService achievementEvaluationService,
                                  SecurityUtils securityUtils) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.achievementEvaluationService = achievementEvaluationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<AchievementDto>> getAll() {
        User user = securityUtils.getCurrentUser();

        // Recalculate progress from current data before returning
        achievementEvaluationService.evaluateAll(user);

        List<Achievement> allAchievements = achievementRepository.findAllByOrderBySortOrderAsc();
        Map<Long, UserAchievement> userMap = userAchievementRepository.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

        List<AchievementDto> result = new ArrayList<>();
        for (Achievement a : allAchievements) {
            AchievementDto dto = new AchievementDto();
            dto.setId(a.getId());
            dto.setKey(a.getKey());
            dto.setName(a.getName());
            dto.setDescription(a.getDescription());
            dto.setIcon(a.getIcon());
            dto.setCategory(a.getCategory().name());
            dto.setThreshold(a.getThreshold());
            dto.setValidFrom(a.getValidFrom());
            dto.setValidUntil(a.getValidUntil());
            dto.setTimeBound(a.isTimeBound());
            dto.setExpired(a.isExpired());
            dto.setActive(a.isActive());

            UserAchievement ua = userMap.get(a.getId());
            if (ua != null) {
                dto.setCurrentValue(ua.getCurrentValue());
                dto.setUnlocked(ua.isUnlocked());
                dto.setUnlockedAt(ua.getUnlockedAt());
                double cv = ua.getCurrentValue() != null ? ua.getCurrentValue() : 0.0;
                dto.setProgress(a.getThreshold() > 0 ? Math.min(cv / a.getThreshold(), 1.0) : 0.0);
            } else {
                dto.setCurrentValue(0.0);
                dto.setUnlocked(false);
                dto.setProgress(0.0);
            }
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AchievementDto>> getRecent() {
        User user = securityUtils.getCurrentUser();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<UserAchievement> recent = userAchievementRepository
                .findByUserIdAndUnlockedAtAfter(user.getId(), sevenDaysAgo);

        List<AchievementDto> result = recent.stream().map(ua -> {
            Achievement a = ua.getAchievement();
            AchievementDto dto = new AchievementDto();
            dto.setId(a.getId());
            dto.setKey(a.getKey());
            dto.setName(a.getName());
            dto.setDescription(a.getDescription());
            dto.setIcon(a.getIcon());
            dto.setCategory(a.getCategory().name());
            dto.setThreshold(a.getThreshold());
            dto.setCurrentValue(ua.getCurrentValue());
            dto.setUnlocked(true);
            dto.setUnlockedAt(ua.getUnlockedAt());
            dto.setProgress(1.0);
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/streak")
    public ResponseEntity<Map<String, Object>> getStreak() {
        User user = securityUtils.getCurrentUser();
        int currentStreak = achievementEvaluationService.getCurrentStreakForUser(user.getId());
        int longestStreak = achievementEvaluationService.getLongestStreakForUser(user.getId());

        return ResponseEntity.ok(Map.of(
                "currentStreak", currentStreak,
                "longestStreak", longestStreak
        ));
    }
}
