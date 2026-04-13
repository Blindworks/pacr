package com.trainingsplan.service;

import com.trainingsplan.entity.Training;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.TrainingPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TrainingService {

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    public List<Training> findAll() {
        return trainingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Training findById(Long id) {
        Training training = trainingRepository.findByIdWithSteps(id).orElse(null);
        if (training == null) {
            return null;
        }

        trainingRepository.findByIdWithPrepTips(id);
        training.getSteps().size();
        training.getPrepTips().size();
        return training;
    }

    @Transactional(readOnly = true)
    public List<Training> findByTrainingPlanId(Long planId) {
        return trainingRepository.findByTrainingPlan_Id(planId);
    }

    public boolean existsById(Long id) {
        return trainingRepository.existsById(id);
    }

    public Training save(Training training) {
        normalizeAndValidatePaceFields(training);
        training.getSteps().forEach(step -> step.setTraining(training));
        training.getPrepTips().forEach(tip -> tip.setTraining(training));
        return trainingRepository.save(training);
    }

    @Transactional
    public Training update(Long id, Training incoming) {
        Training existing = trainingRepository.findById(id).orElseThrow();
        normalizeAndValidatePaceFields(incoming);

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setTrainingType(incoming.getTrainingType());
        existing.setIntensityLevel(incoming.getIntensityLevel());
        existing.setWeekNumber(incoming.getWeekNumber());
        existing.setDayOfWeek(incoming.getDayOfWeek());
        existing.setDurationMinutes(incoming.getDurationMinutes());
        existing.setWorkPace(incoming.getWorkPace());
        existing.setWorkTimeSeconds(incoming.getWorkTimeSeconds());
        existing.setWorkDistanceMeters(incoming.getWorkDistanceMeters());
        existing.setRecoveryPace(incoming.getRecoveryPace());
        existing.setRecoveryTimeSeconds(incoming.getRecoveryTimeSeconds());
        existing.setRecoveryDistanceMeters(incoming.getRecoveryDistanceMeters());
        existing.setIntensityScore(incoming.getIntensityScore());
        existing.setEstimatedCalories(incoming.getEstimatedCalories());
        existing.setBenefit(incoming.getBenefit());
        existing.setEstimatedDistanceMeters(incoming.getEstimatedDistanceMeters());
        existing.setDifficulty(incoming.getDifficulty());
        existing.setHeroImageUrl(incoming.getHeroImageUrl());

        existing.getSteps().clear();
        incoming.getSteps().forEach(step -> {
            step.setTraining(existing);
            existing.getSteps().add(step);
        });

        existing.getPrepTips().clear();
        incoming.getPrepTips().forEach(tip -> {
            tip.setTraining(existing);
            existing.getPrepTips().add(tip);
        });

        return existing;
    }

    public void deleteById(Long id) {
        trainingRepository.deleteById(id);
    }

    private void normalizeAndValidatePaceFields(Training training) {
        String workPace = trimToNull(training.getWorkPace());
        String recoveryPace = trimToNull(training.getRecoveryPace());

        training.setWorkPace(workPace);
        training.setRecoveryPace(recoveryPace);

        Integer workTimeSeconds = training.getWorkTimeSeconds();
        Integer workDistanceMeters = training.getWorkDistanceMeters();
        Integer recoveryTimeSeconds = training.getRecoveryTimeSeconds();
        Integer recoveryDistanceMeters = training.getRecoveryDistanceMeters();

        validatePositiveIfPresent(workTimeSeconds, "work_time_seconds");
        validatePositiveIfPresent(workDistanceMeters, "work_distance_meters");
        validatePositiveIfPresent(recoveryTimeSeconds, "recovery_time_seconds");
        validatePositiveIfPresent(recoveryDistanceMeters, "recovery_distance_meters");

        if (workPace != null) {
            int workPaceSecondsPerKm = parsePaceSecondsPerKm(workPace, "work_pace");
            if (workTimeSeconds == null && workDistanceMeters == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bei work_pace muss work_time_seconds oder work_distance_meters gesetzt sein");
            }
            if (workTimeSeconds == null) {
                training.setWorkTimeSeconds(calculateSecondsFromDistance(workPaceSecondsPerKm, workDistanceMeters));
            } else if (workDistanceMeters == null) {
                training.setWorkDistanceMeters(calculateDistanceFromSeconds(workPaceSecondsPerKm, workTimeSeconds));
            }
        }

        if (recoveryPace != null) {
            int recoveryPaceSecondsPerKm = parsePaceSecondsPerKm(recoveryPace, "recovery_pace");
            if (recoveryTimeSeconds == null && recoveryDistanceMeters == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bei recovery_pace muss recovery_time_seconds oder recovery_distance_meters gesetzt sein");
            }
            if (recoveryTimeSeconds == null) {
                training.setRecoveryTimeSeconds(calculateSecondsFromDistance(recoveryPaceSecondsPerKm, recoveryDistanceMeters));
            } else if (recoveryDistanceMeters == null) {
                training.setRecoveryDistanceMeters(calculateDistanceFromSeconds(recoveryPaceSecondsPerKm, recoveryTimeSeconds));
            }
        }

        Integer effectiveWorkTime = training.getWorkTimeSeconds();
        Integer effectiveRecoveryTime = training.getRecoveryTimeSeconds();
        int totalSeconds = (effectiveWorkTime != null ? effectiveWorkTime : 0)
                + (effectiveRecoveryTime != null ? effectiveRecoveryTime : 0);
        if (totalSeconds > 0) {
            training.setDurationMinutes(Math.max(1, (int) Math.round(totalSeconds / 60.0)));
        }
    }

    private void validatePositiveIfPresent(Integer value, String fieldName) {
        if (value != null && value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " muss > 0 sein");
        }
    }

    private int parsePaceSecondsPerKm(String pace, String fieldName) {
        String[] parts = pace.split(":");
        if (parts.length != 2 && parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " muss im Format MM:SS oder H:MM:SS sein");
        }

        try {
            int hours = 0;
            int minutes;
            int seconds;
            if (parts.length == 2) {
                minutes = Integer.parseInt(parts[0]);
                seconds = Integer.parseInt(parts[1]);
            } else {
                hours = Integer.parseInt(parts[0]);
                minutes = Integer.parseInt(parts[1]);
                seconds = Integer.parseInt(parts[2]);
            }

            if (hours < 0 || minutes < 0 || seconds < 0 || minutes >= 60 || seconds >= 60) {
                throw new NumberFormatException("Out of range");
            }

            int totalSeconds = (hours * 3600) + (minutes * 60) + seconds;
            if (totalSeconds <= 0) {
                throw new NumberFormatException("Must be > 0");
            }
            return totalSeconds;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " muss im Format MM:SS oder H:MM:SS sein");
        }
    }

    private int calculateSecondsFromDistance(int paceSecondsPerKm, Integer distanceMeters) {
        return (int) Math.round((paceSecondsPerKm * distanceMeters) / 1000.0);
    }

    private int calculateDistanceFromSeconds(int paceSecondsPerKm, Integer timeSeconds) {
        return (int) Math.round((timeSeconds * 1000.0) / paceSecondsPerKm);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
