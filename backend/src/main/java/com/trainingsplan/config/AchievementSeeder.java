package com.trainingsplan.config;

import com.trainingsplan.entity.Achievement;
import com.trainingsplan.entity.AchievementDefinition;
import com.trainingsplan.repository.AchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
            achievementRepository.findByKey(def.getKey()).ifPresentOrElse(
                    existing -> {
                        existing.setName(def.getName());
                        existing.setDescription(def.getDescription());
                        existing.setIcon(def.getIcon());
                        existing.setCategory(def.getCategory());
                        existing.setThreshold(def.getThreshold());
                        existing.setSortOrder(def.getSortOrder());
                        achievementRepository.save(existing);
                    },
                    () -> {
                        Achievement a = new Achievement(
                                def.getKey(), def.getName(), def.getDescription(),
                                def.getIcon(), def.getCategory(), def.getThreshold(), def.getSortOrder());
                        achievementRepository.save(a);
                        log.info("Seeded achievement: {}", def.getKey());
                    }
            );
        }
    }
}
