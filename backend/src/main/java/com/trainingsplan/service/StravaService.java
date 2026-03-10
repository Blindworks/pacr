package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.dto.StravaStatusDto;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.StravaToken;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.ActivityStreamRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.StravaTokenRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StravaService {

    private static final Logger log = LoggerFactory.getLogger(StravaService.class);

    @Value("${strava.client-id}")
    private String clientId;

    @Value("${strava.client-secret}")
    private String clientSecret;

    @Value("${strava.redirect-uri}")
    private String redirectUri;

    @Value("${strava.frontend-url}")
    private String frontendUrl;

    private final StravaTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final ActivityMetricsService activityMetricsService;
    private final ActivityMetricsRepository activityMetricsRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final SecurityUtils securityUtils;
    private final UserProfileValidationService userProfileValidationService;
    private final RestClient restClient;

    public StravaService(StravaTokenRepository tokenRepository, ObjectMapper objectMapper,
                         CompletedTrainingRepository completedTrainingRepository,
                         ActivityMetricsService activityMetricsService,
                         ActivityMetricsRepository activityMetricsRepository,
                         ActivityStreamRepository activityStreamRepository,
                         SecurityUtils securityUtils,
                         UserProfileValidationService userProfileValidationService) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.completedTrainingRepository = completedTrainingRepository;
        this.activityMetricsService = activityMetricsService;
        this.activityMetricsRepository = activityMetricsRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.securityUtils = securityUtils;
        this.userProfileValidationService = userProfileValidationService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        factory.setReadTimeout(java.time.Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String getAuthorizationUrl() {
        return "https://www.strava.com/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=activity:read_all";
    }

    public void exchangeCodeForToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");

        try {
            String responseBody = restClient.post()
                    .uri("https://www.strava.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);

            StravaToken token = tokenRepository.findFirstByOrderByIdAsc().orElse(new StravaToken());
            token.setAccessToken(root.path("access_token").asText());
            token.setRefreshToken(root.path("refresh_token").asText());
            token.setExpiresAt(root.path("expires_at").asLong());

            JsonNode athlete = root.path("athlete");
            token.setAthleteId(athlete.path("id").asLong());
            String firstName = athlete.path("firstname").asText("");
            String lastName = athlete.path("lastname").asText("");
            token.setAthleteName((firstName + " " + lastName).trim());
            token.setAthleteCity(athlete.path("city").asText(null));
            token.setProfileMedium(athlete.path("profile_medium").asText(null));

            tokenRepository.save(token);
            log.info("Strava connected: athlete='{}', city='{}'", token.getAthleteName(), token.getAthleteCity());
        } catch (Exception e) {
            log.error("Strava token exchange failed: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange Strava code for token", e);
        }
    }

    private StravaToken refreshTokenIfExpired(StravaToken token) {
        long nowPlusBuffer = System.currentTimeMillis() / 1000 + 300;
        if (token.getExpiresAt() != null && token.getExpiresAt() > nowPlusBuffer) {
            return token;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", token.getRefreshToken());

        try {
            String responseBody = restClient.post()
                    .uri("https://www.strava.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            token.setAccessToken(root.path("access_token").asText());
            token.setRefreshToken(root.path("refresh_token").asText());
            token.setExpiresAt(root.path("expires_at").asLong());
            return tokenRepository.save(token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh Strava token", e);
        }
    }

    public StravaStatusDto getStatus() {
        Optional<StravaToken> tokenOpt = tokenRepository.findFirstByOrderByIdAsc();
        if (tokenOpt.isEmpty()) {
            log.info("Strava status: not connected");
            return new StravaStatusDto(false, null, null, null);
        }
        StravaToken token = tokenOpt.get();
        log.info("Strava status: connected, athlete='{}'", token.getAthleteName());
        return new StravaStatusDto(true, token.getAthleteName(), token.getAthleteCity(), token.getProfileMedium());
    }

    @Transactional
    public List<StravaActivityDto> getActivities(LocalDate start, LocalDate end) {
        Optional<StravaToken> tokenOpt = tokenRepository.findFirstByOrderByIdAsc();
        if (tokenOpt.isEmpty()) {
            return List.of();
        }

        StravaToken token = refreshTokenIfExpired(tokenOpt.get());
        long after = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long before = end.atStartOfDay(ZoneOffset.UTC).toEpochSecond() + 86399;

        try {
            List<StravaActivityDto> activities = fetchAllActivitiesInRange(token.getAccessToken(), after, before);
            User currentUser = securityUtils.getCurrentUser();
            log.info("Strava sync {}/{}: {} activities found, userId={}", start, end, activities.size(),
                    currentUser != null ? currentUser.getId() : "null");
            syncActivitiesToDb(activities, token.getAccessToken(), currentUser);
            removeDeletedActivitiesFromDb(activities, currentUser, start, end);
            return activities;
        } catch (Exception e) {
            log.error("Strava sync failed: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch Strava activities", e);
        }
    }

    private List<StravaActivityDto> fetchAllActivitiesInRange(String accessToken, long after, long before) throws Exception {
        List<StravaActivityDto> allActivities = new ArrayList<>();
        int page = 1;

        while (true) {
            String responseBody = restClient.get()
                    .uri("https://www.strava.com/api/v3/athlete/activities?after=" + after + "&before=" + before + "&per_page=200&page=" + page)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            List<StravaActivityDto> pageItems = objectMapper.readValue(responseBody, new TypeReference<List<StravaActivityDto>>() {});
            if (pageItems.isEmpty()) {
                break;
            }

            allActivities.addAll(pageItems);
            page++;
        }

        return allActivities;
    }

    private void syncActivitiesToDb(List<StravaActivityDto> activities, String accessToken, User user) {
        for (StravaActivityDto dto : activities) {
            if (dto.getId() == null) {
                continue;
            }
            try {
                Optional<CompletedTraining> existing = completedTrainingRepository.findByStravaActivityId(dto.getId());
                if (existing.isPresent()) {
                    CompletedTraining ct = existing.get();
                    boolean dirty = false;
                    // Claim orphaned activity for the current user
                    if (ct.getUser() == null && user != null) {
                        ct.setUser(user);
                        dirty = true;
                    }
                    LocalDateTime startDateTime = parseStravaStartDateTime(dto);
                    if (startDateTime != null && (ct.getUploadDate() == null || !startDateTime.equals(ct.getUploadDate()))) {
                        ct.setUploadDate(startDateTime);
                        dirty = true;
                    }
                    if (dirty) {
                        completedTrainingRepository.save(ct);
                    }
                    continue;
                }

                CompletedTraining ct = convertStravaActivityToCompletedTraining(dto);
                ct.setUser(user);
                CompletedTraining saved = completedTrainingRepository.save(ct);
                fetchStreamsAndPersistMetrics(dto.getId(), saved, accessToken, user);
            } catch (Exception e) {
                log.warn("Failed to persist Strava activity id={}: {}", dto.getId(), e.getMessage());
            }
        }
    }

    private void removeDeletedActivitiesFromDb(List<StravaActivityDto> activities, User user, LocalDate start, LocalDate end) {
        Set<Long> remoteStravaIds = new HashSet<>();
        for (StravaActivityDto activity : activities) {
            if (activity.getId() != null) {
                remoteStravaIds.add(activity.getId());
            }
        }

        List<CompletedTraining> localSyncedActivities;
        if (user != null && user.getId() != null) {
            localSyncedActivities = completedTrainingRepository.findByUserIdAndSourceAndTrainingDateBetween(
                    user.getId(), "STRAVA", start, end);
        } else {
            localSyncedActivities = completedTrainingRepository.findByUserIsNullAndSourceAndTrainingDateBetween(
                    "STRAVA", start, end);
        }

        for (CompletedTraining localActivity : localSyncedActivities) {
            Long localStravaId = localActivity.getStravaActivityId();
            if (localStravaId == null || remoteStravaIds.contains(localStravaId)) {
                continue;
            }

            activityMetricsRepository.deleteByCompletedTrainingId(localActivity.getId());
            activityStreamRepository.deleteByCompletedTrainingId(localActivity.getId());
            completedTrainingRepository.delete(localActivity);
            log.info("Removed local Strava activity {} because it no longer exists on Strava", localStravaId);
        }
    }

    /**
     * Fetches time, heartrate, velocity_smooth, and distance streams from Strava for one activity
     * and persists zone metrics + aerobic decoupling.
     * Silently skips on any error (stream may not exist for activities without HR sensor).
     */
    private void fetchStreamsAndPersistMetrics(Long stravaActivityId, CompletedTraining ct,
                                               String accessToken, User user) {
        try {
            ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
            if (!completion.complete()) {
                log.info("Skipping metric calculation for Strava activity {}. Incomplete profile: {}",
                        stravaActivityId, completion.missingFields());
                return;
            }

            String url = "https://www.strava.com/api/v3/activities/" + stravaActivityId
                    + "/streams?keys=time,heartrate,velocity_smooth,altitude,latlng,distance&key_by_type=true";
            String body = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root     = objectMapper.readTree(body);
            JsonNode timeData = root.path("time").path("data");
            JsonNode hrData   = root.path("heartrate").path("data");
            JsonNode velData  = root.path("velocity_smooth").path("data");
            JsonNode distData = root.path("distance").path("data");
            JsonNode altData  = root.path("altitude").path("data");
            JsonNode latlngNode = root.path("latlng");

            if (timeData.isMissingNode() || !timeData.isArray() || timeData.isEmpty()) return;

            List<Integer> timeSeconds = new ArrayList<>(timeData.size());
            for (JsonNode t : timeData) timeSeconds.add(t.intValue());

            List<Integer> heartRates = new ArrayList<>(timeSeconds.size());
            if (!hrData.isMissingNode() && hrData.isArray() && hrData.size() == timeSeconds.size()) {
                for (JsonNode hr : hrData) heartRates.add(hr.isNull() ? null : hr.intValue());
            } else {
                for (int i = 0; i < timeSeconds.size(); i++) heartRates.add(null);
            }

            List<Double> velocities = new ArrayList<>(timeSeconds.size());
            if (!velData.isMissingNode() && velData.isArray() && velData.size() == timeSeconds.size()) {
                for (JsonNode v : velData) velocities.add(v.isNull() ? null : v.doubleValue());
            } else {
                for (int i = 0; i < timeSeconds.size(); i++) velocities.add(null);
            }

            List<Double> distances = null;
            if (!distData.isMissingNode() && distData.isArray() && distData.size() == timeSeconds.size()) {
                distances = new ArrayList<>(timeSeconds.size());
                for (JsonNode d : distData) distances.add(d.isNull() ? null : d.doubleValue());
            }

            List<Double> altitudes = new ArrayList<>();
            if (!altData.isMissingNode() && altData.isArray()) {
                for (JsonNode a : altData) altitudes.add(a.isNull() ? null : a.doubleValue());
            }

            String latlngJson = null;
            if (!latlngNode.isMissingNode()) {
                latlngJson = objectMapper.writeValueAsString(latlngNode);
            }

            activityMetricsService.calculateAndPersist(ct, timeSeconds, heartRates, velocities, distances, user);
            persistActivityStreams(ct, timeSeconds, heartRates, velocities, altitudes, latlngJson, distances);
        } catch (Exception e) {
            log.warn("Could not fetch/compute streams for Strava activity {}: {}", stravaActivityId, e.getMessage());
        }
    }

    private void persistActivityStreams(CompletedTraining ct, List<Integer> times, List<Integer> heartRates,
                                        List<Double> velocities, List<Double> altitudes, String latlngJson,
                                        List<Double> distances) {
        try {
            ActivityStream stream = activityStreamRepository.findByCompletedTrainingId(ct.getId())
                    .orElseGet(() -> {
                        ActivityStream s = new ActivityStream();
                        s.setCompletedTraining(ct);
                        return s;
                    });

            stream.setTimeSecondsJson(!times.isEmpty() ? objectMapper.writeValueAsString(times) : null);

            boolean hasRealHr = heartRates.stream().anyMatch(hr -> hr != null);
            stream.setHeartrateJson(hasRealHr ? objectMapper.writeValueAsString(heartRates) : null);

            boolean hasRealVel = velocities.stream().anyMatch(v -> v != null);
            stream.setVelocitySmoothJson(hasRealVel ? objectMapper.writeValueAsString(velocities) : null);

            stream.setAltitudeJson(!altitudes.isEmpty() ? objectMapper.writeValueAsString(altitudes) : null);
            stream.setLatlngJson(latlngJson);
            stream.setDistanceJson(distances != null && !distances.isEmpty()
                    ? objectMapper.writeValueAsString(distances) : null);
            stream.setFetchedAt(LocalDateTime.now());

            activityStreamRepository.save(stream);
        } catch (Exception e) {
            log.warn("Could not persist activity streams for completedTrainingId={}: {}", ct.getId(), e.getMessage());
        }
    }

    /**
     * Retroactively computes zone metrics for a CompletedTraining that was synced from Strava.
     * Called on demand (e.g. from the activity dialog) for activities synced before this feature existed.
     */
    public ActivityMetrics computeMetricsForCompletedTraining(Long completedTrainingId) {
        CompletedTraining ct = completedTrainingRepository.findById(completedTrainingId)
                .orElseThrow(() -> new RuntimeException("CompletedTraining not found: " + completedTrainingId));

        if (ct.getStravaActivityId() == null) {
            throw new RuntimeException("Activity " + completedTrainingId + " is not a Strava activity");
        }

        StravaToken token = tokenRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No Strava token found"));
        token = refreshTokenIfExpired(token);

        User user = securityUtils.getCurrentUser();
        if (ct.getUser() == null && user != null) {
            ct.setUser(user);
            ct = completedTrainingRepository.save(ct);
        }
        fetchStreamsAndPersistMetrics(ct.getStravaActivityId(), ct, token.getAccessToken(), user);

        return activityMetricsRepository.findByCompletedTrainingId(completedTrainingId).orElse(null);
    }

    /**
     * Fetches and persists Strava stream data (HR, velocity, altitude, latlng, distance)
     * for a specific completed training. Called on demand from the activity detail view.
     */
    @Transactional
    public void fetchAndPersistStreamsForActivity(Long completedTrainingId) {
        CompletedTraining ct = completedTrainingRepository.findById(completedTrainingId)
                .orElseThrow(() -> new RuntimeException("CompletedTraining not found: " + completedTrainingId));

        if (ct.getStravaActivityId() == null) {
            throw new RuntimeException("Activity " + completedTrainingId + " is not a Strava activity");
        }

        StravaToken token = tokenRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No Strava token found"));
        token = refreshTokenIfExpired(token);

        User user = ct.getUser();
        fetchStreamsAndPersistMetrics(ct.getStravaActivityId(), ct, token.getAccessToken(), user);
    }

    private CompletedTraining convertStravaActivityToCompletedTraining(StravaActivityDto dto) {
        CompletedTraining ct = new CompletedTraining();
        ct.setSource("STRAVA");
        ct.setStravaActivityId(dto.getId());
        ct.setActivityName(dto.getName());

        String sport = dto.getSportType() != null ? dto.getSportType() : dto.getType();
        ct.setSport(sport);

        // Prefer start_date_local (user timezone) over UTC to get the correct calendar date
        String dateStr = dto.getStartDateLocal() != null ? dto.getStartDateLocal() : dto.getStartDate();
        ct.setTrainingDate(LocalDate.parse(dateStr.substring(0, 10)));
        LocalDateTime startDateTime = parseStravaStartDateTime(dto);
        if (startDateTime != null) {
            ct.setUploadDate(startDateTime);
        }

        if (dto.getDistanceMeters() != null && dto.getDistanceMeters() > 0) {
            ct.setDistanceKm(dto.getDistanceMeters() / 1000.0);
        }
        if (dto.getMovingTimeSeconds() != null) {
            ct.setMovingTimeSeconds(dto.getMovingTimeSeconds());
            ct.setDurationSeconds(dto.getMovingTimeSeconds());
        }
        if (dto.getTotalElevationGain() != null && dto.getTotalElevationGain() > 0) {
            ct.setElevationGainM(dto.getTotalElevationGain().intValue());
        }
        if (dto.getAverageSpeed() != null && dto.getAverageSpeed() > 0) {
            ct.setAverageSpeedKmh(dto.getAverageSpeed() * 3.6);
            double paceSecondsPerKm = 1000.0 / dto.getAverageSpeed();
            ct.setAveragePaceSecondsPerKm((int) paceSecondsPerKm);
        }
        if (dto.getMaxSpeed() != null && dto.getMaxSpeed() > 0) {
            ct.setMaxSpeedKmh(dto.getMaxSpeed() * 3.6);
        }
        if (dto.getAverageHeartrate() != null) {
            ct.setAverageHeartRate(dto.getAverageHeartrate().intValue());
        }
        if (dto.getMaxHeartrate() != null) {
            ct.setMaxHeartRate(dto.getMaxHeartrate().intValue());
        }
        if (dto.getAverageWatts() != null) {
            ct.setAveragePowerWatts(dto.getAverageWatts().intValue());
        }

        return ct;
    }

    private LocalDateTime parseStravaStartDateTime(StravaActivityDto dto) {
        String raw = dto.getStartDateLocal() != null ? dto.getStartDateLocal() : dto.getStartDate();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (Exception ignored) {
            // Fall through to LocalDateTime parsing for values without offset
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void disconnect() {
        tokenRepository.deleteAll();
    }
}
