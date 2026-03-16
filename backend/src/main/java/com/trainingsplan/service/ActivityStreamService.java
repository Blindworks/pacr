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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public Optional<ActivityStreamDto> getStreamDto(Long completedTrainingId) {
        Optional<ActivityStream> streamOpt = activityStreamRepository.findByCompletedTrainingId(completedTrainingId);
        if (streamOpt.isEmpty()) return Optional.empty();

        ActivityStream stream = streamOpt.get();
        try {
            List<Double> rawDistances = parseDoubleList(stream.getDistanceJson());
            if (rawDistances == null || rawDistances.isEmpty()) return Optional.empty();

            int n = rawDistances.size();
            int step = Math.max(1, n / MAX_POINTS);
            int sampledCount = (n + step - 1) / step;

            List<Integer> rawHeartRates  = parseIntegerList(stream.getHeartrateJson());
            List<Double>  rawAltitudes   = parseDoubleList(stream.getAltitudeJson());
            List<Double>  rawVelocities  = parseDoubleList(stream.getVelocitySmoothJson());
            List<Integer> rawCadences    = parseIntegerList(stream.getCadenceJson());
            List<Integer> rawPowers      = parseIntegerList(stream.getPowerJson());

            double[]  distancePoints    = new double[sampledCount];
            Integer[] heartRate         = rawHeartRates  != null ? new Integer[sampledCount] : null;
            Double[]  altitude          = rawAltitudes   != null ? new Double[sampledCount]  : null;
            Integer[] paceSecondsPerKm  = rawVelocities  != null ? new Integer[sampledCount] : null;
            Integer[] cadence           = rawCadences    != null ? new Integer[sampledCount] : null;
            Integer[] power             = rawPowers      != null ? new Integer[sampledCount] : null;

            int idx = 0;
            for (int i = 0; i < n; i += step) {
                distancePoints[idx] = rawDistances.get(i) / 1000.0;

                if (rawHeartRates != null && i < rawHeartRates.size())
                    heartRate[idx] = rawHeartRates.get(i);
                if (rawAltitudes != null && i < rawAltitudes.size())
                    altitude[idx] = rawAltitudes.get(i);
                if (rawVelocities != null && i < rawVelocities.size()) {
                    Double v = rawVelocities.get(i);
                    paceSecondsPerKm[idx] = (v != null && v > 0.1) ? (int)(1000.0 / v) : null;
                }
                if (rawCadences != null && i < rawCadences.size())
                    cadence[idx] = rawCadences.get(i);
                if (rawPowers != null && i < rawPowers.size())
                    power[idx] = rawPowers.get(i);

                idx++;
            }

            return Optional.of(new ActivityStreamDto(
                    completedTrainingId,
                    distancePoints,
                    heartRate,
                    altitude,
                    paceSecondsPerKm,
                    cadence,
                    power,
                    anyNonNull(heartRate),
                    anyNonNull(altitude),
                    anyNonNull(paceSecondsPerKm),
                    anyNonNull(cadence),
                    anyNonNull(power)
            ));
        } catch (Exception e) {
            log.error("Failed to parse stream data for completedTrainingId={}: {}", completedTrainingId, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean anyNonNull(Object[] arr) {
        return arr != null && Arrays.stream(arr).anyMatch(Objects::nonNull);
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
