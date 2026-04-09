package com.trainingsplan.repository;

import com.trainingsplan.entity.BotProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BotProfileRepository extends JpaRepository<BotProfile, Long> {

    Optional<BotProfile> findByUserId(Long userId);

    /** Returns all enabled bots whose next scheduled run time has arrived. */
    List<BotProfile> findByEnabledTrueAndNextScheduledRunAtLessThanEqual(LocalDateTime now);
}
