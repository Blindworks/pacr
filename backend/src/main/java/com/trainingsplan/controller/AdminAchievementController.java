package com.trainingsplan.controller;

import com.trainingsplan.dto.AdminAchievementDto;
import com.trainingsplan.entity.Achievement;
import com.trainingsplan.entity.AchievementCategory;
import com.trainingsplan.entity.UserAchievement;
import com.trainingsplan.repository.AchievementRepository;
import com.trainingsplan.repository.UserAchievementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/achievements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAchievementController {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    public AdminAchievementController(AchievementRepository achievementRepository,
                                       UserAchievementRepository userAchievementRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
    }

    @GetMapping
    public ResponseEntity<List<AdminAchievementDto>> getAll() {
        List<Achievement> achievements = achievementRepository.findAllByOrderBySortOrderAsc();
        List<AdminAchievementDto> result = new ArrayList<>();

        for (Achievement a : achievements) {
            AdminAchievementDto dto = toDto(a);
            dto.setUnlockedCount((int) userAchievementRepository.countByAchievementIdAndUnlockedAtIsNotNull(a.getId()));
            dto.setInProgressCount((int) userAchievementRepository.countByAchievementIdAndUnlockedAtIsNull(a.getId()));
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminAchievementDto> getById(@PathVariable Long id) {
        return achievementRepository.findById(id)
                .map(a -> {
                    AdminAchievementDto dto = toDto(a);
                    dto.setUnlockedCount((int) userAchievementRepository.countByAchievementIdAndUnlockedAtIsNotNull(a.getId()));
                    dto.setInProgressCount((int) userAchievementRepository.countByAchievementIdAndUnlockedAtIsNull(a.getId()));

                    List<UserAchievement> userAchievements = userAchievementRepository.findByAchievementId(a.getId());
                    List<AdminAchievementDto.UnlockedUser> users = userAchievements.stream().map(ua -> {
                        AdminAchievementDto.UnlockedUser u = new AdminAchievementDto.UnlockedUser();
                        u.setUserId(ua.getUser().getId());
                        u.setUsername(ua.getUser().getUsername());
                        u.setFirstName(ua.getUser().getFirstName());
                        u.setLastName(ua.getUser().getLastName());
                        u.setCurrentValue(ua.getCurrentValue() != null ? ua.getCurrentValue() : 0.0);
                        u.setUnlockedAt(ua.getUnlockedAt());
                        return u;
                    }).toList();
                    dto.setUnlockedUsers(users);

                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AdminAchievementDto> create(@RequestBody AdminAchievementDto request) {
        Achievement a = new Achievement();
        applyFromRequest(a, request);
        Achievement saved = achievementRepository.save(a);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminAchievementDto> update(@PathVariable Long id, @RequestBody AdminAchievementDto request) {
        return achievementRepository.findById(id)
                .map(a -> {
                    applyFromRequest(a, request);
                    Achievement saved = achievementRepository.save(a);
                    AdminAchievementDto dto = toDto(saved);
                    dto.setUnlockedCount((int) userAchievementRepository.countByAchievementIdAndUnlockedAtIsNotNull(saved.getId()));
                    dto.setInProgressCount((int) userAchievementRepository.countByAchievementIdAndUnlockedAtIsNull(saved.getId()));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!achievementRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        achievementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyFromRequest(Achievement a, AdminAchievementDto req) {
        a.setKey(req.getKey());
        a.setName(req.getName());
        a.setDescription(req.getDescription());
        a.setIcon(req.getIcon());
        a.setCategory(AchievementCategory.valueOf(req.getCategory()));
        a.setThreshold(req.getThreshold());
        a.setSortOrder(req.getSortOrder());
        a.setValidFrom(req.getValidFrom());
        a.setValidUntil(req.getValidUntil());
    }

    private AdminAchievementDto toDto(Achievement a) {
        AdminAchievementDto dto = new AdminAchievementDto();
        dto.setId(a.getId());
        dto.setKey(a.getKey());
        dto.setName(a.getName());
        dto.setDescription(a.getDescription());
        dto.setIcon(a.getIcon());
        dto.setCategory(a.getCategory().name());
        dto.setThreshold(a.getThreshold());
        dto.setSortOrder(a.getSortOrder());
        dto.setValidFrom(a.getValidFrom());
        dto.setValidUntil(a.getValidUntil());
        dto.setTimeBound(a.isTimeBound());
        dto.setActive(a.isActive());
        dto.setExpired(a.isExpired());
        return dto;
    }
}
