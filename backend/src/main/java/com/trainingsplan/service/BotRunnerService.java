package com.trainingsplan.service;

import com.trainingsplan.dto.BotCreateRequest;
import com.trainingsplan.dto.BotProfileDto;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.entity.BotProfile;
import com.trainingsplan.entity.CommunityRoute;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.event.TrainingCompletedEvent;
import com.trainingsplan.repository.ActivityStreamRepository;
import com.trainingsplan.repository.BotProfileRepository;
import com.trainingsplan.repository.CommunityRouteRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles CRUD and execution of bot runner profiles.
 * The {@link #executeBot(BotProfile)} method selects a community route in the bot's
 * radius and generates a plausible CompletedTraining + ActivityStream for that run.
 */
@Service
@Transactional
public class BotRunnerService {

    private static final Logger log = LoggerFactory.getLogger(BotRunnerService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BotProfileRepository botProfileRepository;
    private final UserRepository userRepository;
    private final CommunityRouteRepository communityRouteRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final RouteAttemptService routeAttemptService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    public BotRunnerService(BotProfileRepository botProfileRepository,
                            UserRepository userRepository,
                            CommunityRouteRepository communityRouteRepository,
                            CompletedTrainingRepository completedTrainingRepository,
                            ActivityStreamRepository activityStreamRepository,
                            RouteAttemptService routeAttemptService,
                            ApplicationEventPublisher eventPublisher,
                            PasswordEncoder passwordEncoder) {
        this.botProfileRepository = botProfileRepository;
        this.userRepository = userRepository;
        this.communityRouteRepository = communityRouteRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.routeAttemptService = routeAttemptService;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- CRUD ----------

    @Transactional(readOnly = true)
    public List<BotProfileDto> listBots() {
        return botProfileRepository.findAll().stream()
                .map(BotProfileDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BotProfileDto getBot(Long id) {
        BotProfile bot = botProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found"));
        return BotProfileDto.from(bot);
    }

    public BotProfileDto createBot(BotCreateRequest req) {
        validateRequest(req, true);
        if (userRepository.findByUsername(req.username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.findByEmail(req.email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User botUser = new User();
        botUser.setUsername(req.username);
        botUser.setEmail(req.email);
        botUser.setFirstName(req.firstName);
        botUser.setLastName(req.lastName);
        botUser.setPasswordHash(passwordEncoder.encode(randomPassword()));
        botUser.setRole(UserRole.USER);
        botUser.setStatus(UserStatus.ACTIVE);
        botUser.setCreatedAt(LocalDateTime.now());
        botUser.setBot(true);
        // Bots must have community routes enabled so RouteAttemptService matches their runs.
        botUser.setCommunityRoutesEnabled(true);
        // Mirror coordinates to User so nearby-runner discovery finds the bot.
        botUser.setLatitude(req.homeLatitude);
        botUser.setLongitude(req.homeLongitude);
        botUser.setLocationUpdatedAt(LocalDateTime.now());
        botUser.setDiscoverableByOthers(true);
        botUser.setGender(req.gender);
        botUser.setMaxHeartRate(req.maxHeartRate);
        botUser.setHrRest(req.restingHeartRate);
        User savedUser = userRepository.save(botUser);

        BotProfile bot = new BotProfile();
        bot.setUser(savedUser);
        applyRequest(bot, req);
        bot.setCreatedAt(LocalDateTime.now());
        bot.setUpdatedAt(LocalDateTime.now());
        bot.setNextScheduledRunAt(computeNextScheduledRun(bot, LocalDateTime.now()));

        BotProfile saved = botProfileRepository.save(bot);
        return BotProfileDto.from(saved);
    }

    public BotProfileDto updateBot(Long id, BotCreateRequest req) {
        BotProfile bot = botProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found"));
        validateRequest(req, false);

        User user = bot.getUser();
        if (req.firstName != null) user.setFirstName(req.firstName);
        if (req.lastName != null) user.setLastName(req.lastName);
        if (req.gender != null) user.setGender(req.gender);
        if (req.maxHeartRate != null) user.setMaxHeartRate(req.maxHeartRate);
        if (req.restingHeartRate != null) user.setHrRest(req.restingHeartRate);
        // Keep User coordinates in sync with BotProfile so nearby-runner discovery works.
        if (req.homeLatitude != null && req.homeLongitude != null) {
            user.setLatitude(req.homeLatitude);
            user.setLongitude(req.homeLongitude);
            user.setLocationUpdatedAt(LocalDateTime.now());
        }
        userRepository.save(user);

        applyRequest(bot, req);
        bot.setUpdatedAt(LocalDateTime.now());
        bot.setNextScheduledRunAt(computeNextScheduledRun(bot, LocalDateTime.now()));

        return BotProfileDto.from(botProfileRepository.save(bot));
    }

    public void deleteBot(Long id) {
        BotProfile bot = botProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found"));
        User user = bot.getUser();
        botProfileRepository.delete(bot);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    public BotProfileDto runBotNow(Long id) {
        BotProfile bot = botProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found"));
        executeBot(bot);
        return BotProfileDto.from(bot);
    }

    // ---------- Execution ----------

    /**
     * Runs a single bot: selects a community route, generates a CompletedTraining + ActivityStream,
     * and optionally enters the route leaderboard. Updates bot schedule / status fields.
     * Called from {@link BotRunnerScheduler} and {@link #runBotNow(Long)}.
     */
    public void executeBot(BotProfile bot) {
        LocalDateTime now = LocalDateTime.now();
        try {
            CommunityRoute route = pickRoute(bot);
            if (route == null) {
                bot.setLastRunAt(now);
                bot.setLastRunStatus("NO_ROUTE");
                bot.setLastRunMessage("No matching community route in radius / distance range");
                bot.setNextScheduledRunAt(computeNextScheduledRun(bot, now));
                botProfileRepository.save(bot);
                return;
            }

            User botUser = bot.getUser();
            double distanceKm = route.getDistanceKm() != null ? route.getDistanceKm() : 5.0;
            int paceSecPerKm = randomBetween(bot.getPaceMinSecPerKm(), bot.getPaceMaxSecPerKm());
            int durationSeconds = (int) Math.round(distanceKm * paceSecPerKm);
            double avgSpeedKmh = durationSeconds > 0 ? (distanceKm / (durationSeconds / 3600.0)) : 0.0;

            // Parse GPS track from route (or source activity stream if available)
            List<double[]> latlng = loadRouteLatLng(route);
            int points = latlng.size();

            // Build CompletedTraining
            CompletedTraining ct = new CompletedTraining();
            ct.setUser(botUser);
            ct.setTrainingDate(LocalDate.now());
            ct.setUploadDate(now);
            ct.setStartTime(LocalTime.now().minusSeconds(durationSeconds));
            ct.setDistanceKm(round3(distanceKm));
            ct.setDurationSeconds(durationSeconds);
            ct.setMovingTimeSeconds(durationSeconds);
            ct.setAveragePaceSecondsPerKm(paceSecPerKm);
            ct.setAverageSpeedKmh(round3(avgSpeedKmh));
            ct.setMaxSpeedKmh(round3(avgSpeedKmh * (1.10 + random.nextDouble() * 0.10)));
            ct.setElevationGainM(route.getElevationGainM());
            ct.setSport("running");
            ct.setSource("BOT");
            ct.setActivityName(route.getName());
            ct.setTotalGpsPoints(points);
            if (points > 0) {
                ct.setStartLatitude(latlng.get(0)[0]);
                ct.setStartLongitude(latlng.get(0)[1]);
                ct.setEndLatitude(latlng.get(points - 1)[0]);
                ct.setEndLongitude(latlng.get(points - 1)[1]);
            } else {
                ct.setStartLatitude(route.getStartLatitude());
                ct.setStartLongitude(route.getStartLongitude());
            }

            // HR series + averages
            Integer avgHr = null, maxHr = null, minHr = null;
            int[] hrSeries = null;
            if (bot.getMaxHeartRate() != null && bot.getRestingHeartRate() != null && points > 0) {
                hrSeries = generateHeartRateSeries(bot, points, paceSecPerKm);
                int sum = 0;
                int mx = Integer.MIN_VALUE;
                int mn = Integer.MAX_VALUE;
                for (int hr : hrSeries) {
                    sum += hr;
                    if (hr > mx) mx = hr;
                    if (hr < mn) mn = hr;
                }
                avgHr = Math.round((float) sum / hrSeries.length);
                maxHr = mx;
                minHr = mn;
            }
            ct.setAverageHeartRate(avgHr);
            ct.setMaxHeartRate(maxHr);
            ct.setMinHeartRate(minHr);

            // If leaderboard enabled, open a PENDING attempt before saving the activity
            // so RouteAttemptService.onTrainingCompleted can match it via the published event.
            if (bot.isIncludeInLeaderboard()) {
                try {
                    routeAttemptService.selectRoute(botUser, route.getId());
                } catch (Exception e) {
                    log.warn("Could not open route attempt for bot {}: {}", botUser.getUsername(), e.getMessage());
                }
            }

            CompletedTraining savedCt = completedTrainingRepository.save(ct);

            // Build ActivityStream
            ActivityStream stream = new ActivityStream();
            stream.setCompletedTraining(savedCt);
            stream.setFetchedAt(now);
            if (points > 0) {
                stream.setLatlngJson(writeJson(latlng));
                // time array evenly distributed
                int[] times = new int[points];
                double[] distances = new double[points];
                double distanceStepM = (distanceKm * 1000.0) / Math.max(1, points - 1);
                double timeStep = (double) durationSeconds / Math.max(1, points - 1);
                for (int i = 0; i < points; i++) {
                    times[i] = (int) Math.round(i * timeStep);
                    distances[i] = i * distanceStepM;
                }
                stream.setTimeSecondsJson(writeJson(times));
                stream.setDistanceJson(writeJson(distances));
                if (hrSeries != null) {
                    stream.setHeartrateJson(writeJson(hrSeries));
                }
            }
            activityStreamRepository.save(stream);

            // Publish event so RouteAttemptService (and other listeners) react
            eventPublisher.publishEvent(new TrainingCompletedEvent(this, savedCt, botUser));

            bot.setLastRunAt(now);
            bot.setLastRunStatus("SUCCESS");
            bot.setLastRunMessage("Ran " + route.getName() + " in " + durationSeconds + "s");
            bot.setNextScheduledRunAt(computeNextScheduledRun(bot, now));
            botProfileRepository.save(bot);

        } catch (Exception e) {
            log.error("Bot {} execution failed: {}", bot.getId(), e.getMessage(), e);
            bot.setLastRunAt(now);
            bot.setLastRunStatus("ERROR");
            bot.setLastRunMessage(truncate(e.getMessage(), 490));
            bot.setNextScheduledRunAt(computeNextScheduledRun(bot, now));
            botProfileRepository.save(bot);
        }
    }

    /** Picks a random community route in the bot's radius and distance range, or null. */
    private CommunityRoute pickRoute(BotProfile bot) {
        double radiusKm = bot.getSearchRadiusKm() != null ? bot.getSearchRadiusKm() : 10.0;
        double latDelta = radiusKm / 110.574;
        double lonDelta = radiusKm / (111.320 * Math.cos(Math.toRadians(bot.getHomeLatitude())));

        List<CommunityRoute> routes = communityRouteRepository.findInBoundingBox(
                bot.getHomeLatitude() - latDelta,
                bot.getHomeLatitude() + latDelta,
                bot.getHomeLongitude() - lonDelta,
                bot.getHomeLongitude() + lonDelta
        );

        List<CommunityRoute> matching = routes.stream()
                .filter(r -> r.getDistanceKm() != null)
                .filter(r -> r.getDistanceKm() >= bot.getDistanceMinKm()
                          && r.getDistanceKm() <= bot.getDistanceMaxKm())
                .filter(r -> haversineKm(bot.getHomeLatitude(), bot.getHomeLongitude(),
                                         r.getStartLatitude(), r.getStartLongitude()) <= radiusKm)
                .collect(Collectors.toList());

        if (matching.isEmpty()) return null;
        return matching.get(random.nextInt(matching.size()));
    }

    /** Loads lat/lng from the route's gpsTrackJson (or its source activity stream as fallback). */
    @SuppressWarnings("unchecked")
    private List<double[]> loadRouteLatLng(CommunityRoute route) {
        List<double[]> out = new ArrayList<>();
        try {
            if (route.getGpsTrackJson() != null && !route.getGpsTrackJson().isBlank()) {
                List<List<Number>> parsed = MAPPER.readValue(route.getGpsTrackJson(),
                        new TypeReference<List<List<Number>>>() {});
                for (List<Number> pt : parsed) {
                    if (pt != null && pt.size() >= 2) {
                        out.add(new double[]{pt.get(0).doubleValue(), pt.get(1).doubleValue()});
                    }
                }
            }
            if (out.isEmpty() && route.getSourceActivity() != null) {
                Optional<ActivityStream> srcOpt = activityStreamRepository
                        .findByCompletedTrainingId(route.getSourceActivity().getId());
                if (srcOpt.isPresent() && srcOpt.get().getLatlngJson() != null) {
                    List<List<Number>> parsed = MAPPER.readValue(srcOpt.get().getLatlngJson(),
                            new TypeReference<List<List<Number>>>() {});
                    for (List<Number> pt : parsed) {
                        if (pt != null && pt.size() >= 2) {
                            out.add(new double[]{pt.get(0).doubleValue(), pt.get(1).doubleValue()});
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse GPS track for route {}: {}", route.getId(), e.getMessage());
        }
        return out;
    }

    /** Plausible HR series: starts near resting, ramps up, oscillates around target. */
    private int[] generateHeartRateSeries(BotProfile bot, int points, int paceSecPerKm) {
        int resting = bot.getRestingHeartRate();
        int max = bot.getMaxHeartRate();
        // Faster pace -> higher fraction of HR reserve used
        double effort = Math.max(0.55, Math.min(0.90, 1.10 - (paceSecPerKm / 500.0)));
        int target = (int) Math.round(resting + (max - resting) * effort);
        int warmupPoints = Math.min(points / 10, 30);
        int[] hr = new int[points];
        for (int i = 0; i < points; i++) {
            int base;
            if (i < warmupPoints && warmupPoints > 0) {
                base = resting + (int) ((target - resting) * ((double) i / warmupPoints));
            } else {
                base = target;
            }
            int noise = random.nextInt(7) - 3;
            hr[i] = Math.max(resting, Math.min(max, base + noise));
        }
        return hr;
    }

    // ---------- Schedule ----------

    /**
     * Computes the next run time: find the next day whose DayOfWeek is in the set,
     * at scheduleStartTime ± jitter. Returns null if schedule incomplete.
     */
    public LocalDateTime computeNextScheduledRun(BotProfile bot, LocalDateTime from) {
        Set<DayOfWeek> days = bot.getScheduleDaySet();
        LocalTime time = bot.getScheduleStartTime();
        if (days.isEmpty() || time == null) return null;

        LocalDate probe = from.toLocalDate();
        for (int i = 0; i < 14; i++) {
            LocalDate day = probe.plusDays(i);
            if (days.contains(day.getDayOfWeek())) {
                LocalDateTime candidate = LocalDateTime.of(day, time);
                int jitter = bot.getScheduleJitterMinutes() == null ? 0 : bot.getScheduleJitterMinutes();
                if (jitter > 0) {
                    int offset = random.nextInt(2 * jitter + 1) - jitter;
                    candidate = candidate.plusMinutes(offset);
                }
                if (candidate.isAfter(from)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    // ---------- Helpers ----------

    private void validateRequest(BotCreateRequest req, boolean createMode) {
        if (createMode) {
            if (isBlank(req.username)) throw new IllegalArgumentException("username is required");
            if (isBlank(req.email)) throw new IllegalArgumentException("email is required");
        }
        if (req.homeLatitude == null || req.homeLongitude == null) {
            throw new IllegalArgumentException("homeLatitude and homeLongitude are required");
        }
        if (req.paceMinSecPerKm == null || req.paceMaxSecPerKm == null
                || req.paceMinSecPerKm <= 0 || req.paceMaxSecPerKm < req.paceMinSecPerKm) {
            throw new IllegalArgumentException("Invalid pace range");
        }
        if (req.distanceMinKm == null || req.distanceMaxKm == null
                || req.distanceMinKm <= 0 || req.distanceMaxKm < req.distanceMinKm) {
            throw new IllegalArgumentException("Invalid distance range");
        }
    }

    private void applyRequest(BotProfile bot, BotCreateRequest req) {
        if (req.cityName != null) bot.setCityName(req.cityName);
        bot.setHomeLatitude(req.homeLatitude);
        bot.setHomeLongitude(req.homeLongitude);
        bot.setSearchRadiusKm(req.searchRadiusKm != null ? req.searchRadiusKm : 10.0);
        if (req.gender != null) bot.setGender(req.gender);
        if (req.age != null) bot.setAge(req.age);
        bot.setPaceMinSecPerKm(req.paceMinSecPerKm);
        bot.setPaceMaxSecPerKm(req.paceMaxSecPerKm);
        bot.setDistanceMinKm(req.distanceMinKm);
        bot.setDistanceMaxKm(req.distanceMaxKm);
        if (req.maxHeartRate != null) bot.setMaxHeartRate(req.maxHeartRate);
        if (req.restingHeartRate != null) bot.setRestingHeartRate(req.restingHeartRate);
        if (req.scheduleDays != null) {
            Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
            for (String d : req.scheduleDays) {
                try { set.add(DayOfWeek.valueOf(d.toUpperCase())); }
                catch (IllegalArgumentException ignored) {}
            }
            bot.setScheduleDaySet(set);
        }
        if (req.scheduleStartTime != null) bot.setScheduleStartTime(req.scheduleStartTime);
        if (req.scheduleJitterMinutes != null) bot.setScheduleJitterMinutes(req.scheduleJitterMinutes);
        if (req.includeInLeaderboard != null) bot.setIncludeInLeaderboard(req.includeInLeaderboard);
        if (req.enabled != null) bot.setEnabled(req.enabled);
    }

    private int randomBetween(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    private String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private String randomPassword() {
        byte[] buf = new byte[24];
        new java.security.SecureRandom().nextBytes(buf);
        return "Bot!" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
