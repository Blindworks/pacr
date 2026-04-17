package com.trainingsplan.config;

import com.trainingsplan.entity.Achievement;
import com.trainingsplan.entity.AchievementDefinition;
import com.trainingsplan.repository.AchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds default achievements on startup. Insert-only: never overwrites
 * achievements that already exist, so admin edits via the UI are preserved.
 */
@Component
public class AchievementSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AchievementSeeder.class);

    private final AchievementRepository achievementRepository;

    public AchievementSeeder(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    @Override
    public void run(String... args) {
        for (AchievementDefinition def : AchievementDefinition.values()) {
            if (achievementRepository.findByKey(def.getKey()).isPresent()) {
                continue;
            }
            Achievement a = new Achievement(
                    def.getKey(), def.getName(), def.getDescription(),
                    def.getIcon(), def.getCategory(), def.getMetric(),
                    def.getThreshold(), def.getSortOrder());
            achievementRepository.save(a);
            log.info("Seeded achievement: {}", def.getKey());
        }
    }
}
