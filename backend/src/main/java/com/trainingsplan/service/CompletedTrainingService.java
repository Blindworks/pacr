package com.trainingsplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garmin.fit.*;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityStreamRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CompletedTrainingService {

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;

    @Autowired
    private UserTrainingScheduleService userTrainingScheduleService;

    @Autowired
    private BodyMetricService bodyMetricService;

    @Autowired
    private ActivityMetricsService activityMetricsService;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private GpxParsingService gpxParsingService;

    @Autowired
    private TcxParsingService tcxParsingService;

    @Autowired
    private ActivityStreamRepository activityStreamRepository;

    @Autowired
    private PersonalRecordService personalRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogService auditLogService;

    private static final Logger log = LoggerFactory.getLogger(CompletedTrainingService.class);

    /**
     * Dispatches file upload to the correct parser based on the file extension.
     * Supported formats: .fit, .gpx, .tcx
     */
    public CompletedTraining uploadAndParseFile(MultipartFile file, LocalDate trainingDate, Long trainingId) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".fit")) {
            return uploadAndParseFitFile(file, trainingDate, trainingId);
        } else if (filename.endsWith(".gpx")) {
            return uploadAndParseGpxFile(file, trainingDate, trainingId);
        } else if (filename.endsWith(".tcx")) {
            return uploadAndParseTcxFile(file, trainingDate, trainingId);
        } else {
            throw new IOException("Nicht unterstütztes Dateiformat. Erlaubt: .fit, .gpx, .tcx");
        }
    }

    private CompletedTraining uploadAndParseGpxFile(MultipartFile file, LocalDate trainingDate, Long trainingId) throws IOException {
        ParsedActivityData data;
        try {
            data = gpxParsingService.parse(file.getBytes());
        } catch (Exception e) {
            throw new IOException("Fehler beim Parsen der GPX-Datei: " + e.getMessage(), e);
        }
        CompletedTraining training = data.training;
        // File date has priority; use parameter only if the file contained no date
        if (training.getTrainingDate() == null) training.setTrainingDate(trainingDate);
        training.setOriginalFilename(file.getOriginalFilename());
        training.setUploadDate(LocalDateTime.now());

        User currentUser = securityUtils.getCurrentUser();
        training.setUser(currentUser);
        CompletedTraining savedTraining = completedTrainingRepository.save(training);

        auditLogService.log(currentUser, AuditAction.FIT_UPLOADED, "COMPLETED_TRAINING",
                String.valueOf(savedTraining.getId()), null);

        bodyMetricService.calculateAndStore(savedTraining, currentUser);
        activityMetricsService.calculateAndPersist(savedTraining, data.timeSeconds, data.heartRates, currentUser);

        try {
            personalRecordService.checkAndUpdateFromActivity(currentUser, savedTraining);
        } catch (Exception e) {
            log.warn("Could not check personal records: {}", e.getMessage());
        }

        if (trainingId != null) {
            try { userTrainingScheduleService.updateCompletion(trainingId, true, "completed"); }
            catch (Exception ignored) {}
        }
        return savedTraining;
    }

    private CompletedTraining uploadAndParseTcxFile(MultipartFile file, LocalDate trainingDate, Long trainingId) throws IOException {
        ParsedActivityData data;
        try {
            data = tcxParsingService.parse(file.getBytes());
        } catch (Exception e) {
            throw new IOException("Fehler beim Parsen der TCX-Datei: " + e.getMessage(), e);
        }
        CompletedTraining training = data.training;
        // File date has priority; use parameter only if the file contained no date
        if (training.getTrainingDate() == null) training.setTrainingDate(trainingDate);
        training.setOriginalFilename(file.getOriginalFilename());
        training.setUploadDate(LocalDateTime.now());

        User currentUser = securityUtils.getCurrentUser();
        training.setUser(currentUser);
        CompletedTraining savedTraining = completedTrainingRepository.save(training);

        auditLogService.log(currentUser, AuditAction.FIT_UPLOADED, "COMPLETED_TRAINING",
                String.valueOf(savedTraining.getId()), null);

        bodyMetricService.calculateAndStore(savedTraining, currentUser);
        activityMetricsService.calculateAndPersist(savedTraining, data.timeSeconds, data.heartRates, currentUser);

        try {
            personalRecordService.checkAndUpdateFromActivity(currentUser, savedTraining);
        } catch (Exception e) {
            log.warn("Could not check personal records: {}", e.getMessage());
        }

        if (trainingId != null) {
            try { userTrainingScheduleService.updateCompletion(trainingId, true, "completed"); }
            catch (Exception ignored) {}
        }
        return savedTraining;
    }

    public CompletedTraining uploadAndParseFitFile(MultipartFile file, LocalDate trainingDate) throws IOException {
        return uploadAndParseFitFile(file, trainingDate, null);
    }

    public CompletedTraining uploadAndParseFitFile(MultipartFile file, LocalDate trainingDate, Long trainingId) throws IOException {
        CompletedTraining training = new CompletedTraining();
        training.setOriginalFilename(file.getOriginalFilename());
        training.setUploadDate(LocalDateTime.now());

        FitDataCollector collector;
        try {
            collector = parseFitFileReal(file.getBytes(), training);
        } catch (Exception e) {
            throw new IOException("Fehler beim Parsen der FIT-Datei: " + e.getMessage(), e);
        }

        // Prefer the date extracted from the FIT file; fall back to the request parameter
        LocalDate effectiveDate = collector.getParsedStartDate() != null
                ? collector.getParsedStartDate()
                : trainingDate;
        training.setTrainingDate(effectiveDate);

        User currentUser = securityUtils.getCurrentUser();
        training.setUser(currentUser);
        CompletedTraining savedTraining = completedTrainingRepository.save(training);

        auditLogService.log(currentUser, AuditAction.FIT_UPLOADED, "COMPLETED_TRAINING",
                String.valueOf(savedTraining.getId()), null);

        saveActivityStream(savedTraining, collector);

        // Calculate and persist body metrics (VO2Max etc.)
        bodyMetricService.calculateAndStore(savedTraining, currentUser);

        // Calculate and persist HR zone metrics from the collected stream
        activityMetricsService.calculateAndPersist(
                savedTraining,
                collector.getTimeSeconds(),
                collector.getHeartRates(),
                currentUser);

        try {
            personalRecordService.checkAndUpdateFromActivity(currentUser, savedTraining);
        } catch (Exception e) {
            log.warn("Could not check personal records: {}", e.getMessage());
        }

        // If trainingId is provided, mark the UserTrainingEntry as completed
        if (trainingId != null) {
            try {
                userTrainingScheduleService.updateCompletion(trainingId, true, "completed");
            } catch (Exception ignored) {
                // Entry may not exist (legacy case); silently ignore
            }
        }

        return savedTraining;
    }

    private FitDataCollector parseFitFileReal(byte[] fitData, CompletedTraining training) {
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);

        FitDataCollector collector = new FitDataCollector(training);
        mesgBroadcaster.addListener(collector);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(fitData);

        if (!decode.checkFileIntegrity(inputStream)) {
            throw new RuntimeException("FIT-Datei ist beschädigt oder ungültig");
        }

        inputStream = new ByteArrayInputStream(fitData);
        try {
            decode.read(inputStream, mesgBroadcaster, mesgBroadcaster);
            collector.finalizeData();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der FIT-Datei: " + e.getMessage(), e);
        }

        return collector;
    }

    private static class FitDataCollector implements MesgListener {
        private final CompletedTraining training;

        // HR stream: raw FIT timestamps (seconds since Garmin epoch) and bpm values
        private final List<Long> rawTimestamps = new ArrayList<>();
        private final List<Integer> rawHeartRates = new ArrayList<>();

        // Additional per-record streams — parallel to rawTimestamps (null where field absent)
        private final List<Double>  rawDistances = new ArrayList<>();
        private final List<Double>  rawAltitudes = new ArrayList<>();
        private final List<Double>  rawSpeeds    = new ArrayList<>();  // m/s
        private final List<Integer> rawCadences  = new ArrayList<>();
        private final List<Integer> rawPowers    = new ArrayList<>();
        private final List<Double>  rawLats      = new ArrayList<>();  // degrees
        private final List<Double>  rawLons      = new ArrayList<>();  // degrees

        // Semicircle → degree conversion factor
        private static final double SEMICIRCLE_TO_DEG = 180.0 / Math.pow(2, 31);

        // Computed after finalizeData()
        private List<Integer> timeSeconds = new ArrayList<>();
        private List<Integer> heartRates = new ArrayList<>();
        private List<Double>  distances  = new ArrayList<>();
        private List<Double>  altitudes  = new ArrayList<>();
        private List<Double>  speeds     = new ArrayList<>();
        private List<Integer> cadences   = new ArrayList<>();
        private List<Integer> powers     = new ArrayList<>();
        private List<Double>  latitudes  = new ArrayList<>();
        private List<Double>  longitudes = new ArrayList<>();

        /** Activity start date parsed from the FIT file (may be null if not present). */
        private LocalDate parsedStartDate = null;

        /** FIT epoch offset: seconds between 1989-12-31T00:00:00Z and 1970-01-01T00:00:00Z. */
        private static final long FIT_EPOCH_OFFSET = 631065600L;

        public FitDataCollector(CompletedTraining training) {
            this.training = training;
        }

        @Override
        public void onMesg(Mesg mesg) {
            switch (mesg.getName()) {
                case "activity":    handleActivity(mesg);   break;
                case "session":     handleSession(mesg);    break;
                case "device_info": handleDeviceInfo(mesg); break;
                case "record":      handleRecord(mesg);     break;
            }
        }

        private void handleRecord(Mesg mesg) {
            Field tsField = mesg.getField("timestamp");
            if (tsField == null || tsField.getValue() == null) return;
            Long ts = tsField.getLongValue();
            if (ts == null) return;

            rawTimestamps.add(ts);

            // Heart rate
            rawHeartRates.add(extractInt(mesg, "heart_rate"));

            // Distance (meters cumulative)
            rawDistances.add(extractDouble(mesg, "distance"));

            // Altitude (meters)
            rawAltitudes.add(extractDouble(mesg, "altitude"));

            // Speed (m/s)
            rawSpeeds.add(extractDouble(mesg, "speed"));

            // Cadence (spm/rpm)
            rawCadences.add(extractInt(mesg, "cadence"));

            // Power (watts)
            rawPowers.add(extractInt(mesg, "power"));

            // GPS coordinates (semicircles → degrees)
            Double latSc = extractDouble(mesg, "position_lat");
            Double lonSc = extractDouble(mesg, "position_long");
            rawLats.add(latSc != null ? latSc * SEMICIRCLE_TO_DEG : null);
            rawLons.add(lonSc != null ? lonSc * SEMICIRCLE_TO_DEG : null);
        }

        private Double extractDouble(Mesg mesg, String fieldName) {
            Field f = mesg.getField(fieldName);
            return (f != null && f.getValue() != null) ? f.getDoubleValue() : null;
        }

        private Integer extractInt(Mesg mesg, String fieldName) {
            Field f = mesg.getField(fieldName);
            if (f == null || f.getValue() == null) return null;
            Double v = f.getDoubleValue();
            return v != null ? v.intValue() : null;
        }

        private void handleActivity(Mesg mesg) {
            setFieldValue(mesg, "total_distance",    val -> training.setDistanceKm(val / 1000.0));
            setFieldValue(mesg, "total_timer_time",  val -> training.setDurationSeconds(val.intValue()));
            setFieldValue(mesg, "total_moving_time", val -> training.setMovingTimeSeconds(val.intValue()));
            setFieldValue(mesg, "total_calories",    val -> training.setCalories(val.intValue()));
            setFieldValue(mesg, "total_ascent",      val -> training.setElevationGainM(val.intValue()));
            setFieldValue(mesg, "total_descent",     val -> training.setElevationLossM(val.intValue()));
        }

        private void handleSession(Mesg mesg) {
            // Extract activity start date from the session's start_time field
            Field startTimeField = mesg.getField("start_time");
            if (startTimeField != null && startTimeField.getValue() != null) {
                Long fitTs = startTimeField.getLongValue();
                if (fitTs != null && fitTs > 0) {
                    long unixSeconds = fitTs + FIT_EPOCH_OFFSET;
                    parsedStartDate = LocalDateTime
                            .ofEpochSecond(unixSeconds, 0, ZoneOffset.UTC)
                            .toLocalDate();
                }
            }

            setFieldString(mesg, "sport",     val -> training.setSport(val.toLowerCase()));
            setFieldString(mesg, "sub_sport", val -> training.setSubSport(val.toLowerCase()));

            setFieldValue(mesg, "avg_speed",        val -> training.setAverageSpeedKmh(val * 3.6));
            setFieldValue(mesg, "max_speed",        val -> training.setMaxSpeedKmh(val * 3.6));
            setFieldValue(mesg, "avg_heart_rate",   val -> training.setAverageHeartRate(val.intValue()));
            setFieldValue(mesg, "max_heart_rate",   val -> training.setMaxHeartRate(val.intValue()));
            setFieldValue(mesg, "min_heart_rate",   val -> training.setMinHeartRate(val.intValue()));
            setFieldValue(mesg, "avg_power",        val -> training.setAveragePowerWatts(val.intValue()));
            setFieldValue(mesg, "max_power",        val -> training.setMaxPowerWatts(val.intValue()));
            setFieldValue(mesg, "normalized_power", val -> training.setNormalizedPowerWatts(val.intValue()));
            setFieldValue(mesg, "avg_cadence",      val -> training.setAverageCadence(val.intValue()));
            setFieldValue(mesg, "max_cadence",      val -> training.setMaxCadence(val.intValue()));
            setFieldValue(mesg, "avg_temperature",  val -> training.setTemperatureCelsius(val));
            setFieldValue(mesg, "avg_running_cadence", val -> training.setAverageCadence(val.intValue()));
            setFieldValue(mesg, "max_running_cadence", val -> training.setMaxCadence(val.intValue()));

            // Fallbacks
            if (training.getDistanceKm() == null)
                setFieldValue(mesg, "total_distance",    val -> training.setDistanceKm(val / 1000.0));
            if (training.getDurationSeconds() == null)
                setFieldValue(mesg, "total_timer_time",  val -> training.setDurationSeconds(val.intValue()));
            if (training.getMovingTimeSeconds() == null)
                setFieldValue(mesg, "total_moving_time", val -> training.setMovingTimeSeconds(val.intValue()));
            if (training.getCalories() == null)
                setFieldValue(mesg, "total_calories",    val -> training.setCalories(val.intValue()));
            if (training.getElevationGainM() == null)
                setFieldValue(mesg, "total_ascent",      val -> training.setElevationGainM(val.intValue()));
            if (training.getElevationLossM() == null)
                setFieldValue(mesg, "total_descent",     val -> training.setElevationLossM(val.intValue()));
        }

        private void handleDeviceInfo(Mesg mesg) {
            setFieldString(mesg, "manufacturer",     val -> training.setDeviceManufacturer(val));
            setFieldString(mesg, "product",          val -> training.setDeviceProduct(val));
            setFieldString(mesg, "serial_number",    val -> training.setDeviceSerialNumber(val));
            setFieldString(mesg, "software_version", val -> training.setSoftwareVersion(val));
        }

        private void setFieldValue(Mesg mesg, String fieldName, java.util.function.Consumer<Double> setter) {
            Field field = mesg.getField(fieldName);
            if (field != null && field.getValue() != null) {
                Double value = field.getDoubleValue();
                if (value != null) setter.accept(value);
            }
        }

        private void setFieldString(Mesg mesg, String fieldName, java.util.function.Consumer<String> setter) {
            Field field = mesg.getField(fieldName);
            if (field != null && field.getValue() != null) {
                String value = field.getValue().toString();
                if (!value.isEmpty()) setter.accept(value);
            }
        }

        public void finalizeData() {
            // Compute pace if not yet set
            if (training.getAveragePaceSecondsPerKm() == null
                    && training.getDistanceKm() != null && training.getDurationSeconds() != null
                    && training.getDurationSeconds() > 0 && training.getDistanceKm() > 0) {
                double paceSecondsPerKm = training.getDurationSeconds() / training.getDistanceKm();
                training.setAveragePaceSecondsPerKm((int) paceSecondsPerKm);
            }

            // Convert absolute timestamps to relative seconds since first record
            if (!rawTimestamps.isEmpty()) {
                long t0 = rawTimestamps.get(0);
                timeSeconds = new ArrayList<>(rawTimestamps.size());
                heartRates  = new ArrayList<>(rawHeartRates.size());
                for (int i = 0; i < rawTimestamps.size(); i++) {
                    timeSeconds.add((int)(rawTimestamps.get(i) - t0));
                    heartRates.add(rawHeartRates.get(i)); // may be null
                }
            }

            // Build output lists aligned with timeSeconds
            distances  = new ArrayList<>(rawDistances);
            altitudes  = new ArrayList<>(rawAltitudes);
            speeds     = new ArrayList<>(rawSpeeds);
            cadences   = new ArrayList<>(rawCadences);
            powers     = new ArrayList<>(rawPowers);
            latitudes  = new ArrayList<>(rawLats);
            longitudes = new ArrayList<>(rawLons);

            // Distance fallback for indoor/treadmill FIT files (no GPS distance)
            boolean distanceAllNull = distances.stream().allMatch(Objects::isNull);
            if (distanceAllNull) {
                // Derive cumulative distance from speed × Δt
                boolean hasAnySpeed = speeds.stream().anyMatch(Objects::nonNull);
                if (hasAnySpeed) {
                    double cumulative = 0.0;
                    for (int i = 0; i < rawTimestamps.size(); i++) {
                        Double spd = speeds.get(i);
                        if (spd != null && i > 0) {
                            long dt = rawTimestamps.get(i) - rawTimestamps.get(i - 1);
                            cumulative += spd * dt;
                        }
                        distances.set(i, cumulative);  // 0.0 at i=0 is valid (start of activity)
                    }
                }
                // If no speed either, distances remains all-null → ActivityStream will not be saved
            }
        }

        /** Relative seconds since activity start (same indices as {@link #getHeartRates()}). */
        public List<Integer> getTimeSeconds() { return timeSeconds; }

        /** HR in bpm per sample; entries may be null for GPS-only ticks. */
        public List<Integer> getHeartRates()  { return heartRates; }

        /** Start date from the FIT file's session message, or {@code null} if not present. */
        public LocalDate getParsedStartDate() { return parsedStartDate; }

        public List<Double>  getDistances()  { return distances; }
        public List<Double>  getAltitudes()  { return altitudes; }
        public List<Double>  getSpeeds()     { return speeds; }
        public List<Integer> getCadences()   { return cadences; }
        public List<Integer> getPowers()     { return powers; }
        public List<Double>  getLatitudes()  { return latitudes; }
        public List<Double>  getLongitudes() { return longitudes; }
    }

    /**
     * Serializes a list to JSON. Returns null if the list is null, empty, or contains only nulls
     * (to avoid storing meaningless [null,null,...] arrays that cause NPE during stream parsing).
     */
    private String toJson(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        boolean allNull = list.stream().allMatch(Objects::isNull);
        if (allNull) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to serialize stream list: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Builds [[lat,lon],...] JSON, skipping pairs where either coordinate is null.
     */
    private String buildLatlngJson(List<Double> lats, List<Double> lons) {
        if (lats == null || lons == null || lats.isEmpty()) return null;
        java.util.List<double[]> pairs = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(lats.size(), lons.size()); i++) {
            Double lat = lats.get(i);
            Double lon = lons.get(i);
            if (lat != null && lon != null) {
                pairs.add(new double[]{lat, lon});
            }
        }
        if (pairs.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(pairs);
        } catch (Exception e) {
            log.warn("Failed to serialize latlng stream: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Saves an ActivityStream for the given completed training from FIT collector data.
     * Skipped if distanceJson would be null (service requires distance as x-axis).
     */
    private void saveActivityStream(CompletedTraining savedTraining, FitDataCollector collector) {
        try {
            String distanceJson = toJson(collector.getDistances());
            if (distanceJson == null) {
                log.debug("No distance stream for activity {}, skipping ActivityStream save", savedTraining.getId());
                return;
            }
            ActivityStream stream = new ActivityStream();
            stream.setCompletedTraining(savedTraining);
            stream.setFetchedAt(LocalDateTime.now());
            stream.setDistanceJson(distanceJson);
            stream.setTimeSecondsJson(toJson(collector.getTimeSeconds()));
            stream.setHeartrateJson(toJson(collector.getHeartRates()));
            stream.setAltitudeJson(toJson(collector.getAltitudes()));
            stream.setVelocitySmoothJson(toJson(collector.getSpeeds()));
            stream.setCadenceJson(toJson(collector.getCadences()));
            stream.setPowerJson(toJson(collector.getPowers()));
            stream.setLatlngJson(buildLatlngJson(collector.getLatitudes(), collector.getLongitudes()));
            activityStreamRepository.save(stream);
        } catch (Exception e) {
            log.warn("Failed to save ActivityStream for completedTrainingId={}: {}", savedTraining.getId(), e.getMessage());
        }
    }

    public List<CompletedTraining> getCompletedTrainingsByDate(LocalDate date) {
        return completedTrainingRepository.findByTrainingDateOrderByUploadDateDesc(date);
    }

    public List<CompletedTraining> getCompletedTrainingsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return completedTrainingRepository.findByTrainingDateBetweenOrderByTrainingDate(startDate, endDate);
    }

    public Optional<CompletedTraining> getLatestRunningTrainingForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return completedTrainingRepository
                .findTopByUserIdAndSportContainingIgnoreCaseOrderByTrainingDateDescUploadDateDesc(userId, "run");
    }
}
