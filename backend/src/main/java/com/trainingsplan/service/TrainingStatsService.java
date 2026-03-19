package com.trainingsplan.service;

import com.trainingsplan.dto.TrainingStatsDto;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
public class TrainingStatsService {

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    public TrainingStatsDto getStats(Long userId, String period, LocalDate from, LocalDate to, String trainingType, String sport) {
        LocalDate today = LocalDate.now();
        if (from == null) {
            from = resolveFromDate(period, today);
        }
        if (to == null) {
            to = resolveToDate(period, today, from);
        }

        List<CompletedTraining> trainings = completedTrainingRepository
                .findByUserIdAndTrainingDateBetweenOrderByTrainingDate(userId, from, to);

        trainings = applyFilters(trainings, trainingType, sport);

        List<TrainingStatsDto.Bucket> buckets = buildBuckets(period, trainings, from, to);

        double totalDistanceKm = buckets.stream().mapToDouble(TrainingStatsDto.Bucket::getDistanceKm).sum();
        int totalDurationSeconds = buckets.stream().mapToInt(TrainingStatsDto.Bucket::getDurationSeconds).sum();
        int totalActivityCount = buckets.stream().mapToInt(TrainingStatsDto.Bucket::getActivityCount).sum();

        totalDistanceKm = Math.round(totalDistanceKm * 100.0) / 100.0;

        List<Long> trainingIds = trainings.stream().map(CompletedTraining::getId).toList();
        List<ActivityMetrics> metricsList = trainingIds.isEmpty()
            ? List.of()
            : activityMetricsRepository.findByCompletedTrainingIdIn(trainingIds);

        long totalZone1Seconds = Math.round(metricsList.stream()
            .filter(am -> !Boolean.TRUE.equals(am.getZonesUnknown()) && am.getZ1Min() != null)
            .mapToDouble(ActivityMetrics::getZ1Min).sum() * 60);
        long totalZone2Seconds = Math.round(metricsList.stream()
            .filter(am -> !Boolean.TRUE.equals(am.getZonesUnknown()) && am.getZ2Min() != null)
            .mapToDouble(ActivityMetrics::getZ2Min).sum() * 60);
        long totalZone3Seconds = Math.round(metricsList.stream()
            .filter(am -> !Boolean.TRUE.equals(am.getZonesUnknown()) && am.getZ3Min() != null)
            .mapToDouble(ActivityMetrics::getZ3Min).sum() * 60);
        long totalZone4Seconds = Math.round(metricsList.stream()
            .filter(am -> !Boolean.TRUE.equals(am.getZonesUnknown()) && am.getZ4Min() != null)
            .mapToDouble(ActivityMetrics::getZ4Min).sum() * 60);
        long totalZone5Seconds = Math.round(metricsList.stream()
            .filter(am -> !Boolean.TRUE.equals(am.getZonesUnknown()) && am.getZ5Min() != null)
            .mapToDouble(ActivityMetrics::getZ5Min).sum() * 60);

        int totalElevationGainM = trainings.stream()
                .mapToInt(ct -> Optional.ofNullable(ct.getElevationGainM()).orElse(0))
                .sum();

        OptionalDouble avgHrOpt = trainings.stream()
                .filter(ct -> ct.getAverageHeartRate() != null)
                .mapToInt(CompletedTraining::getAverageHeartRate)
                .average();
        Double avgHeartRate = avgHrOpt.isPresent()
                ? Math.round(avgHrOpt.getAsDouble() * 10.0) / 10.0
                : null;

        double avgPaceSecondsPerKm = totalDistanceKm > 0
                ? (double) totalDurationSeconds / totalDistanceKm
                : 0.0;

        TrainingStatsDto dto = new TrainingStatsDto(buckets, totalDistanceKm, totalDurationSeconds, totalActivityCount);
        dto.setAvgPaceSecondsPerKm(Math.round(avgPaceSecondsPerKm * 10.0) / 10.0);
        dto.setTotalZone1Seconds(totalZone1Seconds);
        dto.setTotalZone2Seconds(totalZone2Seconds);
        dto.setTotalZone3Seconds(totalZone3Seconds);
        dto.setTotalZone4Seconds(totalZone4Seconds);
        dto.setTotalZone5Seconds(totalZone5Seconds);
        dto.setTotalElevationGainM(totalElevationGainM);
        dto.setAvgHeartRate(avgHeartRate);
        return dto;
    }

    private LocalDate resolveFromDate(String period, LocalDate today) {
        if (period == null) {
            return today.minusMonths(12);
        }
        return switch (period) {
            case "day"          -> today.minusDays(29);
            case "week"         -> today.minusWeeks(12);
            case "currentWeek"  -> today.minusDays(today.getDayOfWeek().getValue() - 1L);
            case "lastWeek"     -> today.minusDays(today.getDayOfWeek().getValue() - 1L + 7L);
            case "currentMonth" -> today.withDayOfMonth(1);
            case "currentYear"  -> today.withDayOfYear(1);
            case "year"         -> LocalDate.of(2000, 1, 1);
            case "all"          -> LocalDate.of(2000, 1, 1);
            default             -> today.minusMonths(12); // "month" and unknown
        };
    }

    private LocalDate resolveToDate(String period, LocalDate today, LocalDate from) {
        if ("currentWeek".equals(period) || "lastWeek".equals(period)) {
            return from.plusDays(6);
        }
        return today;
    }

    private List<CompletedTraining> applyFilters(List<CompletedTraining> trainings,
                                                  String trainingType, String sport) {
        List<CompletedTraining> result = trainings;

        if (trainingType != null && !trainingType.isBlank()) {
            result = result.stream()
                    .filter(ct -> trainingType.equalsIgnoreCase(ct.getTrainingType()))
                    .toList();
        }

        if (sport != null && !sport.isBlank()) {
            result = result.stream()
                    .filter(ct -> sport.equalsIgnoreCase(ct.getSport()))
                    .toList();
        }

        return result;
    }

    private List<TrainingStatsDto.Bucket> buildBuckets(String period,
                                                        List<CompletedTraining> trainings,
                                                        LocalDate from, LocalDate to) {
        if ("all".equals(period)) {
            return buildAllBucket(trainings, from, to);
        }
        if ("day".equals(period) || "currentWeek".equals(period) || "lastWeek".equals(period) || "currentMonth".equals(period)) {
            return buildDayBuckets(trainings, from, to);
        }
        if ("week".equals(period)) {
            return buildWeekBuckets(trainings, from, to);
        }
        if ("year".equals(period) || "currentYear".equals(period)) {
            return buildYearBuckets(trainings, from, to);
        }
        // default: month
        return buildMonthBuckets(trainings, from, to);
    }

    // -------------------------------------------------------------------------
    // "all" — single aggregate bucket
    // -------------------------------------------------------------------------

    private List<TrainingStatsDto.Bucket> buildAllBucket(List<CompletedTraining> trainings,
                                                          LocalDate from, LocalDate to) {
        double distanceKm = 0.0;
        int durationSeconds = 0;
        int elevationGainM = 0;

        for (CompletedTraining ct : trainings) {
            distanceKm    += Optional.ofNullable(ct.getDistanceKm()).orElse(0.0);
            durationSeconds += Optional.ofNullable(ct.getDurationSeconds()).orElse(0);
            elevationGainM  += Optional.ofNullable(ct.getElevationGainM()).orElse(0);
        }

        distanceKm = Math.round(distanceKm * 100.0) / 100.0;

        TrainingStatsDto.Bucket bucket = new TrainingStatsDto.Bucket(
                "Gesamt",
                from.toString(),
                to.toString(),
                distanceKm,
                durationSeconds,
                elevationGainM,
                trainings.size()
        );

        return List.of(bucket);
    }

    // -------------------------------------------------------------------------
    // "day" — one bucket per day (last 30 days)
    // -------------------------------------------------------------------------

    private List<TrainingStatsDto.Bucket> buildDayBuckets(List<CompletedTraining> trainings,
                                                           LocalDate from, LocalDate to) {
        Map<LocalDate, TrainingStatsDto.Bucket> bucketMap = new LinkedHashMap<>();

        LocalDate day = from;
        while (!day.isAfter(to)) {
            bucketMap.put(day, new TrainingStatsDto.Bucket(
                    dayLabel(day),
                    day.toString(),
                    day.toString(),
                    0.0, 0, 0, 0
            ));
            day = day.plusDays(1);
        }

        for (CompletedTraining ct : trainings) {
            TrainingStatsDto.Bucket bucket = bucketMap.get(ct.getTrainingDate());
            if (bucket != null) {
                accumulate(bucket, ct);
            }
        }

        return new ArrayList<>(bucketMap.values());
    }

    private String dayLabel(LocalDate date) {
        return String.format("%02d.%02d.", date.getDayOfMonth(), date.getMonthValue());
    }

    // -------------------------------------------------------------------------
    // "week" — group by ISO week
    // -------------------------------------------------------------------------

    private List<TrainingStatsDto.Bucket> buildWeekBuckets(List<CompletedTraining> trainings,
                                                             LocalDate from, LocalDate to) {
        // Key: "YYYY-WW" — preserves insertion order via LinkedHashMap
        Map<String, TrainingStatsDto.Bucket> bucketMap = new LinkedHashMap<>();

        // Pre-populate all weeks in range so empty weeks appear in result
        LocalDate weekStart = from.minusDays(from.getDayOfWeek().getValue() - 1);
        while (!weekStart.isAfter(to)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            String key = isoWeekKey(weekStart);
            if (!bucketMap.containsKey(key)) {
                bucketMap.put(key, new TrainingStatsDto.Bucket(
                        weekLabel(weekStart),
                        weekStart.toString(),
                        weekEnd.isAfter(to) ? to.toString() : weekEnd.toString(),
                        0.0, 0, 0, 0
                ));
            }
            weekStart = weekStart.plusWeeks(1);
        }

        for (CompletedTraining ct : trainings) {
            LocalDate date = ct.getTrainingDate();
            String key = isoWeekKey(date);
            TrainingStatsDto.Bucket bucket = bucketMap.get(key);
            if (bucket == null) {
                // Activity outside the pre-populated range — skip (shouldn't happen)
                continue;
            }
            accumulate(bucket, ct);
        }

        return new ArrayList<>(bucketMap.values());
    }

    private String isoWeekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return year + "-" + String.format("%02d", week);
    }

    private String weekLabel(LocalDate mondayOfWeek) {
        int week = mondayOfWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = mondayOfWeek.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("KW %d %d", week, year);
    }

    // -------------------------------------------------------------------------
    // "month" — group by YearMonth
    // -------------------------------------------------------------------------

    private List<TrainingStatsDto.Bucket> buildMonthBuckets(List<CompletedTraining> trainings,
                                                              LocalDate from, LocalDate to) {
        Map<YearMonth, TrainingStatsDto.Bucket> bucketMap = new LinkedHashMap<>();

        // Pre-populate all months in range
        YearMonth current = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!current.isAfter(end)) {
            LocalDate monthStart = current.atDay(1);
            LocalDate monthEnd   = current.atEndOfMonth();
            bucketMap.put(current, new TrainingStatsDto.Bucket(
                    monthLabel(current),
                    monthStart.toString(),
                    monthEnd.isAfter(to) ? to.toString() : monthEnd.toString(),
                    0.0, 0, 0, 0
            ));
            current = current.plusMonths(1);
        }

        for (CompletedTraining ct : trainings) {
            YearMonth ym = YearMonth.from(ct.getTrainingDate());
            TrainingStatsDto.Bucket bucket = bucketMap.get(ym);
            if (bucket == null) {
                continue;
            }
            accumulate(bucket, ct);
        }

        return new ArrayList<>(bucketMap.values());
    }

    private String monthLabel(YearMonth ym) {
        String monthName = Month.of(ym.getMonthValue()).getDisplayName(TextStyle.FULL, Locale.GERMAN);
        return monthName + " " + ym.getYear();
    }

    // -------------------------------------------------------------------------
    // "year" — group by year
    // -------------------------------------------------------------------------

    private List<TrainingStatsDto.Bucket> buildYearBuckets(List<CompletedTraining> trainings,
                                                             LocalDate from, LocalDate to) {
        Map<Integer, TrainingStatsDto.Bucket> bucketMap = new LinkedHashMap<>();

        // Only pre-populate years that actually contain data (avoids showing 2000–2024 empties)
        for (CompletedTraining ct : trainings) {
            int year = ct.getTrainingDate().getYear();
            bucketMap.computeIfAbsent(year, y -> new TrainingStatsDto.Bucket(
                    String.valueOf(y),
                    LocalDate.of(y, 1, 1).toString(),
                    LocalDate.of(y, 12, 31).isAfter(to) ? to.toString() : LocalDate.of(y, 12, 31).toString(),
                    0.0, 0, 0, 0
            ));
            accumulate(bucketMap.get(year), ct);
        }

        return new ArrayList<>(bucketMap.values());
    }

    // -------------------------------------------------------------------------
    // Shared accumulator
    // -------------------------------------------------------------------------

    private void accumulate(TrainingStatsDto.Bucket bucket, CompletedTraining ct) {
        double newDistance = bucket.getDistanceKm()
                + Optional.ofNullable(ct.getDistanceKm()).orElse(0.0);
        newDistance = Math.round(newDistance * 100.0) / 100.0;
        bucket.setDistanceKm(newDistance);

        bucket.setDurationSeconds(bucket.getDurationSeconds()
                + Optional.ofNullable(ct.getDurationSeconds()).orElse(0));

        bucket.setElevationGainM(bucket.getElevationGainM()
                + Optional.ofNullable(ct.getElevationGainM()).orElse(0));

        bucket.setActivityCount(bucket.getActivityCount() + 1);
    }
}
