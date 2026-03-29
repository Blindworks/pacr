package com.trainingsplan.service;

import com.trainingsplan.dto.CorosSportData;
import com.trainingsplan.dto.CorosWebhookRequest;
import com.trainingsplan.entity.CorosToken;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.event.TrainingCompletedEvent;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.CorosTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class CorosWebhookService {

    private static final Logger log = LoggerFactory.getLogger(CorosWebhookService.class);

    @Value("${coros.webhook.client:}")
    private String expectedClient;

    @Value("${coros.webhook.secret:}")
    private String expectedSecret;

    private final CorosTokenRepository corosTokenRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MetricsKernelService metricsKernelService;

    public CorosWebhookService(CorosTokenRepository corosTokenRepository,
                               CompletedTrainingRepository completedTrainingRepository,
                               ApplicationEventPublisher eventPublisher,
                               MetricsKernelService metricsKernelService) {
        this.corosTokenRepository = corosTokenRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.eventPublisher = eventPublisher;
        this.metricsKernelService = metricsKernelService;
    }

    public boolean validateRequest(String client, String secret) {
        if (expectedClient.isEmpty() || expectedSecret.isEmpty()) {
            log.warn("COROS webhook credentials not configured, accepting request");
            return true;
        }
        return expectedClient.equals(client) && expectedSecret.equals(secret);
    }

    @Transactional
    public void processWorkoutData(CorosWebhookRequest request) {
        List<CorosSportData> sportDataList = request.getSportDataList();
        if (sportDataList == null || sportDataList.isEmpty()) {
            log.info("COROS webhook received empty sportDataList");
            return;
        }

        log.info("COROS webhook received {} workout(s)", sportDataList.size());

        for (CorosSportData data : sportDataList) {
            try {
                processSingleWorkout(data);
            } catch (Exception e) {
                log.error("Failed to process COROS workout labelId={}: {}", data.getLabelId(), e.getMessage());
            }
        }
    }

    private void processSingleWorkout(CorosSportData data) {
        String labelId = data.getLabelId();
        if (labelId == null || labelId.isBlank()) {
            log.warn("COROS workout with null labelId, skipping");
            return;
        }

        // Duplicate check
        if (completedTrainingRepository.existsByCorosLabelId(labelId)) {
            log.debug("COROS workout labelId={} already exists, skipping", labelId);
            return;
        }

        // Find linked user via openId
        User user = null;
        if (data.getOpenId() != null) {
            Optional<CorosToken> tokenOpt = corosTokenRepository.findByOpenId(data.getOpenId());
            if (tokenOpt.isPresent()) {
                user = tokenOpt.get().getUser();
            } else {
                log.warn("COROS webhook: no token found for openId={}", data.getOpenId());
            }
        }

        CompletedTraining ct = convertToCompletedTraining(data);
        ct.setUser(user);
        CompletedTraining saved = completedTrainingRepository.save(ct);

        log.info("COROS workout saved: labelId={}, sport={}, date={}, userId={}",
                labelId, ct.getSport(), ct.getTrainingDate(),
                user != null ? user.getId() : "null");

        // Post-processing
        eventPublisher.publishEvent(new TrainingCompletedEvent(this, saved, user));

        if (user != null && saved.getTrainingDate() != null) {
            try {
                metricsKernelService.computeForDate(user, saved.getTrainingDate());
            } catch (Exception e) {
                log.warn("Metrics computation failed for COROS workout labelId={}: {}", labelId, e.getMessage());
            }
        }
    }

    private CompletedTraining convertToCompletedTraining(CorosSportData data) {
        CompletedTraining ct = new CompletedTraining();
        ct.setSource("COROS");
        ct.setCorosLabelId(data.getLabelId());

        // Sport type mapping
        ct.setSport(CorosSportTypeMapper.getSportName(data.getMode(), data.getSubMode()));
        ct.setSubSport(CorosSportTypeMapper.getParentSport(data.getMode()));

        // Device info
        ct.setDeviceManufacturer("COROS");
        ct.setDeviceProduct(data.getDeviceName());

        // Training date from start time + timezone
        if (data.getStartTime() != null) {
            ZoneOffset offset = timezoneToOffset(data.getStartTimezone());
            LocalDate trainingDate = Instant.ofEpochSecond(data.getStartTime())
                    .atOffset(offset)
                    .toLocalDate();
            ct.setTrainingDate(trainingDate);
            ct.setUploadDate(Instant.ofEpochSecond(data.getStartTime())
                    .atOffset(offset)
                    .toLocalDateTime());
        } else {
            ct.setTrainingDate(LocalDate.now());
        }

        // Duration
        if (data.getStartTime() != null && data.getEndTime() != null) {
            ct.setDurationSeconds((int) (data.getEndTime() - data.getStartTime()));
            ct.setMovingTimeSeconds((int) (data.getEndTime() - data.getStartTime()));
        }

        // Distance (COROS sends meters, we store km)
        if (data.getDistance() != null && data.getDistance() > 0) {
            ct.setDistanceKm(data.getDistance() / 1000.0);
        }

        // Pace (COROS avgSpeed is seconds/km)
        if (data.getAvgSpeed() != null && data.getAvgSpeed() > 0) {
            ct.setAveragePaceSecondsPerKm(data.getAvgSpeed());
            ct.setAverageSpeedKmh(3600.0 / data.getAvgSpeed());
        }

        // Cadence (COROS avgFrequency is steps/min)
        if (data.getAvgFrequency() != null && data.getAvgFrequency() > 0) {
            ct.setAverageCadence(data.getAvgFrequency());
        }

        // Calories (COROS sends in calorie unit - based on example values like 9553, this appears to be kcal)
        if (data.getCalorie() != null && data.getCalorie() > 0) {
            ct.setCalories(data.getCalorie().intValue());
        }

        // Activity name from sport type
        ct.setActivityName(CorosSportTypeMapper.getSportName(data.getMode(), data.getSubMode()));

        return ct;
    }

    /**
     * Converts COROS 15-minute timezone system to ZoneOffset.
     * COROS uses a system where 32 = UTC+08:00 (32 * 15min = 480min = 8h).
     * 0 = UTC+00:00, negative values for west of UTC.
     */
    private ZoneOffset timezoneToOffset(Integer corosTimezone) {
        if (corosTimezone == null) {
            return ZoneOffset.UTC;
        }
        int totalMinutes = corosTimezone * 15;
        try {
            return ZoneOffset.ofTotalSeconds(totalMinutes * 60);
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }
}
