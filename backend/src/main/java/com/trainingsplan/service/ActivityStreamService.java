package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ActivityStreamDto;
import com.trainingsplan.dto.GpsStreamDto;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.repository.ActivityStreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ActivityStreamService {

    private static final Logger log = LoggerFactory.getLogger(ActivityStreamService.class);
    private static final int MAX_POINTS = 200;
    private static final int MAX_GPS_POINTS = 500;
    private static final int LONG_RUN_SECONDS = 7200; // 2 hours

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

    public Optional<GpsStreamDto> getGpsStreamDto(Long completedTrainingId) {
        Optional<ActivityStream> streamOpt = activityStreamRepository.findByCompletedTrainingId(completedTrainingId);
        if (streamOpt.isEmpty()) return Optional.empty();

        ActivityStream stream = streamOpt.get();
        try {
            List<double[]> rawLatlng = parseLatlngJson(stream.getLatlngJson());
            if (rawLatlng == null || rawLatlng.isEmpty()) return Optional.empty();

            List<Double> rawDistances  = parseDoubleList(stream.getDistanceJson());
            List<Integer> rawHeartRates = parseIntegerList(stream.getHeartrateJson());
            List<Double> rawVelocities = parseDoubleList(stream.getVelocitySmoothJson());
            List<Double> rawAltitudes  = parseDoubleList(stream.getAltitudeJson());
            List<Integer> rawTimes     = parseIntegerList(stream.getTimeSecondsJson());

            int n = rawLatlng.size();

            // Determine sampling step: for long runs (>2h) use every 5th, otherwise cap at MAX_GPS_POINTS
            boolean isLongRun = rawTimes != null && !rawTimes.isEmpty()
                    && rawTimes.get(rawTimes.size() - 1) != null
                    && rawTimes.get(rawTimes.size() - 1) > LONG_RUN_SECONDS;
            int step = isLongRun ? 5 : Math.max(1, n / MAX_GPS_POINTS);

            // Collect valid (non-null latlng) sampled points
            List<double[]> sampledLatlng = new ArrayList<>();
            List<Double> sampledDist = new ArrayList<>();
            List<Integer> sampledHr = new ArrayList<>();
            List<Integer> sampledPace = new ArrayList<>();
            List<Double> sampledAlt = new ArrayList<>();

            for (int i = 0; i < n; i += step) {
                double[] ll = rawLatlng.get(i);
                if (ll == null || ll.length < 2 || ll[0] == 0.0 && ll[1] == 0.0) continue;

                sampledLatlng.add(ll);
                sampledDist.add(rawDistances != null && i < rawDistances.size() ? rawDistances.get(i) : 0.0);

                if (rawHeartRates != null && i < rawHeartRates.size()) {
                    sampledHr.add(rawHeartRates.get(i));
                } else {
                    sampledHr.add(null);
                }

                if (rawVelocities != null && i < rawVelocities.size()) {
                    Double v = rawVelocities.get(i);
                    sampledPace.add(v != null && v > 0.1 ? (int) (1000.0 / v) : null);
                } else {
                    sampledPace.add(null);
                }

                if (rawAltitudes != null && i < rawAltitudes.size()) {
                    sampledAlt.add(rawAltitudes.get(i));
                } else {
                    sampledAlt.add(null);
                }
            }

            if (sampledLatlng.isEmpty()) return Optional.empty();

            int count = sampledLatlng.size();
            double[][] latlng = sampledLatlng.toArray(new double[count][]);
            double[] distance = sampledDist.stream().mapToDouble(d -> d != null ? d : 0.0).toArray();
            Integer[] heartRate = sampledHr.toArray(new Integer[count]);
            Integer[] paceSecondsPerKm = sampledPace.toArray(new Integer[count]);
            Double[] altitude = sampledAlt.toArray(new Double[count]);

            return Optional.of(new GpsStreamDto(
                    completedTrainingId,
                    latlng,
                    distance,
                    heartRate,
                    paceSecondsPerKm,
                    altitude,
                    anyNonNull(heartRate),
                    anyNonNull(paceSecondsPerKm),
                    anyNonNull(altitude)
            ));
        } catch (Exception e) {
            log.error("Failed to parse GPS stream for completedTrainingId={}: {}", completedTrainingId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses latlng JSON handling both formats:
     * - FIT files: [[lat,lng], ...]
     * - Strava: {"data":[[lat,lng],...], "series_type":"latlng", ...}
     */
    private List<double[]> parseLatlngJson(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        JsonNode root = objectMapper.readTree(json);

        JsonNode dataNode;
        if (root.isArray()) {
            dataNode = root;
        } else if (root.isObject() && root.has("data")) {
            dataNode = root.get("data");
        } else {
            return null;
        }

        if (!dataNode.isArray()) return null;

        List<double[]> result = new ArrayList<>(dataNode.size());
        for (JsonNode point : dataNode) {
            if (point.isArray() && point.size() >= 2 && !point.get(0).isNull() && !point.get(1).isNull()) {
                result.add(new double[]{point.get(0).doubleValue(), point.get(1).doubleValue()});
            } else {
                result.add(null);
            }
        }
        return result;
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
