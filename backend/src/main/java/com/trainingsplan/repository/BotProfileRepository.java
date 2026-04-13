package com.trainingsplan.repository;

import com.trainingsplan.entity.BotProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BotProfileRepository extends JpaRepository<BotProfile, Long> {

    Optional<BotProfile> findByUserId(Long userId);

    /** Returns all enabled bots whose next scheduled run time has arrived, with User eagerly fetched. */
    @Query("SELECT b FROM BotProfile b JOIN FETCH b.user WHERE b.enabled = true AND b.nextScheduledRunAt <= :now")
    List<BotProfile> findDueBotsWithUser(@Param("now") LocalDateTime now);
}
