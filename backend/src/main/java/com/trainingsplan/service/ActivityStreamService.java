package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ActivityStreamDto;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.repository.ActivityStreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Converts persisted raw Strava stream JSON into downsampled {@link ActivityStreamDto} objects
 * suitable for chart rendering. Downsampling is applied to keep at most 200 data points.
 */
@Service
@Transactional(readOnly = true)
public class ActivityStreamService {

    private static final Logger log = LoggerFactory.getLogger(ActivityStreamService.class);
    private static final int MAX_POINTS = 200;

    private final ActivityStreamRepository activityStreamRepository;
    private final ObjectMapper objectMapper;

    public ActivityStreamService(ActivityStreamRepository activityStreamRepository,
                                 ObjectMapper objectMapper) {
        this.activityStreamRepository = activityStreamRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns a downsampled stream DTO for the given completed training, or empty if no
     * stream data has been persisted yet.
     */
    public Optional<ActivityStreamDto> getStreamDto(Long completedTrainingId) {
        Optional<ActivityStream> streamOpt = activityStreamRepository.findByCompletedTrainingId(completedTrainingId);
        if (streamOpt.isEmpty()) {
            return Optional.empty();
        }

        ActivityStream stream = streamOpt.get();

        try {
            List<Double> rawDistances = parseDoubleList(stream.getDistanceJson());
            if (rawDistances == null || rawDistances.isEmpty()) {
                return Optional.empty();
            }

            int n = rawDistances.size();
            int step = Math.max(1, n / MAX_POINTS);

            List<Integer> rawHeartRates = stream.getHeartrateJson() != null && !stream.getHeartrateJson().isBlank()
                    ? parseIntegerList(stream.getHeartrateJson()) : null;
            List<Double> rawAltitudes = stream.getAltitudeJson() != null && !stream.getAltitudeJson().isBlank()
                    ? parseDoubleList(stream.getAltitudeJson()) : null;
            List<Double> rawVelocities = stream.getVelocitySmoothJson() != null && !stream.getVelocitySmoothJson().isBlank()
                    ? parseDoubleList(stream.getVelocitySmoothJson()) : null;

            int sampledCount = (n + step - 1) / step;
            double[] distancePoints = new double[sampledCount];
            Integer[] heartRate = rawHeartRates != null ? new Integer[sampledCount] : null;
            Double[] altitude = rawAltitudes != null ? new Double[sampledCount] : null;
            Integer[] paceSecondsPerKm = rawVelocities != null ? new Integer[sampledCount] : null;

            int idx = 0;
            for (int i = 0; i < n; i += step) {
                distancePoints[idx] = rawDistances.get(i) / 1000.0;

                if (rawHeartRates != null && i < rawHeartRates.size()) {
                    heartRate[idx] = rawHeartRates.get(i);
                }
                if (rawAltitudes != null && i < rawAltitudes.size()) {
                    altitude[idx] = rawAltitudes.get(i);
                }
                if (rawVelocities != null && i < rawVelocities.size()) {
                    Double v = rawVelocities.get(i);
                    paceSecondsPerKm[idx] = (v != null && v > 0.1) ? (int) (1000.0 / v) : null;
                }
                idx++;
            }

            boolean hasHeartRate = stream.getHeartrateJson() != null && !stream.getHeartrateJson().isBlank();
            boolean hasAltitude = stream.getAltitudeJson() != null && !stream.getAltitudeJson().isBlank();
            boolean hasPace = stream.getVelocitySmoothJson() != null && !stream.getVelocitySmoothJson().isBlank();

            return Optional.of(new ActivityStreamDto(
                    completedTrainingId,
                    distancePoints,
                    heartRate,
                    altitude,
                    paceSecondsPerKm,
                    hasHeartRate,
                    hasAltitude,
                    hasPace
            ));
        } catch (Exception e) {
            log.error("Failed to parse stream data for completedTrainingId={}: {}", completedTrainingId, e.getMessage());
            return Optional.empty();
        }
    }

    private List<Double> parseDoubleList(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
    }

    private List<Integer> parseIntegerList(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
    }
}
