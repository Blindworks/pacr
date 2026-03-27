package com.trainingsplan.repository;

import com.trainingsplan.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

    Optional<UserAchievement> findByUserIdAndAchievementKey(Long userId, String key);

    boolean existsByUserIdAndAchievementIdAndUnlockedAtIsNotNull(Long userId, Long achievementId);

    long countByUserIdAndUnlockedAtIsNotNull(Long userId);

    List<UserAchievement> findByUserIdAndUnlockedAtAfter(Long userId, LocalDateTime after);

    List<UserAchievement> findByAchievementId(Long achievementId);

    long countByAchievementIdAndUnlockedAtIsNotNull(Long achievementId);

    long countByAchievementIdAndUnlockedAtIsNull(Long achievementId);
}
