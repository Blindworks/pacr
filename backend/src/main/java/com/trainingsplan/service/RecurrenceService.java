package com.trainingsplan.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecurrenceService {

    /**
     * Expands an RRULE string into a list of occurrence dates within the given range.
     * Supports: FREQ=DAILY, FREQ=WEEKLY (with BYDAY, INTERVAL), FREQ=MONTHLY (with BYDAY positional like 2TU),
     * FREQ=YEARLY (with BYMONTH, BYDAY).
     */
    public List<LocalDate> expandOccurrences(String rrule, LocalDate seriesStart,
                                              LocalDate rangeStart, LocalDate rangeEnd,
                                              Set<LocalDate> exceptions) {
        if (rrule == null || rrule.isBlank()) {
            return List.of();
        }

        Map<String, String> parts = parseRrule(rrule);
        String freq = parts.getOrDefault("FREQ", "");
        int interval = Integer.parseInt(parts.getOrDefault("INTERVAL", "1"));
        LocalDate until = parts.containsKey("UNTIL") ? LocalDate.parse(parts.get("UNTIL")) : null;

        LocalDate effectiveEnd = rangeEnd;
        if (until != null && until.isBefore(effectiveEnd)) {
            effectiveEnd = until;
        }

        List<LocalDate> dates = switch (freq) {
            case "DAILY" -> expandDaily(seriesStart, rangeStart, effectiveEnd, interval);
            case "WEEKLY" -> expandWeekly(seriesStart, rangeStart, effectiveEnd, interval, parts.get("BYDAY"));
            case "MONTHLY" -> expandMonthly(seriesStart, rangeStart, effectiveEnd, interval, parts.get("BYDAY"));
            case "YEARLY" -> expandYearly(seriesStart, rangeStart, effectiveEnd, interval, parts.get("BYMONTH"), parts.get("BYDAY"));
            default -> List.of();
        };

        if (exceptions != null && !exceptions.isEmpty()) {
            return dates.stream()
                    .filter(d -> !exceptions.contains(d))
                    .toList();
        }
        return dates;
    }

    /**
     * Builds an RRULE string from structured parameters.
     */
    public String buildRrule(String frequency, Integer interval, List<String> byDay,
                              Integer bySetPos, LocalDate until) {
        StringBuilder sb = new StringBuilder("FREQ=").append(frequency);
        if (interval != null && interval > 1) {
            sb.append(";INTERVAL=").append(interval);
        }
        if (byDay != null && !byDay.isEmpty()) {
            if (bySetPos != null && "MONTHLY".equals(frequency)) {
                // For monthly: encode position into BYDAY like "2TU"
                sb.append(";BYDAY=").append(bySetPos).append(byDay.get(0));
            } else {
                sb.append(";BYDAY=").append(String.join(",", byDay));
            }
        }
        if (until != null) {
            sb.append(";UNTIL=").append(until);
        }
        return sb.toString();
    }

    private Map<String, String> parseRrule(String rrule) {
        Map<String, String> map = new HashMap<>();
        for (String part : rrule.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    private List<LocalDate> expandDaily(LocalDate start, LocalDate rangeStart, LocalDate rangeEnd, int interval) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(rangeEnd)) {
            if (!current.isBefore(rangeStart)) {
                dates.add(current);
            }
            current = current.plusDays(interval);
        }
        return dates;
    }

    private List<LocalDate> expandWeekly(LocalDate start, LocalDate rangeStart, LocalDate rangeEnd,
                                          int interval, String byDay) {
        Set<DayOfWeek> days = parseDays(byDay);
        if (days.isEmpty()) {
            days = Set.of(start.getDayOfWeek());
        }

        List<LocalDate> dates = new ArrayList<>();
        // Find the first week start (Monday of the series start week)
        LocalDate weekStart = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int weekCounter = 0;

        while (!weekStart.isAfter(rangeEnd)) {
            if (weekCounter % interval == 0) {
                for (DayOfWeek day : days) {
                    LocalDate date = weekStart.with(TemporalAdjusters.nextOrSame(day));
                    if (!date.isBefore(start) && !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)) {
                        dates.add(date);
                    }
                }
            }
            weekStart = weekStart.plusWeeks(1);
            weekCounter++;
        }

        dates.sort(Comparator.naturalOrder());
        return dates;
    }

    private List<LocalDate> expandMonthly(LocalDate start, LocalDate rangeStart, LocalDate rangeEnd,
                                           int interval, String byDay) {
        List<LocalDate> dates = new ArrayList<>();

        // Parse BYDAY like "2TU" (2nd Tuesday) or "-1FR" (last Friday)
        int position = 0;
        DayOfWeek dayOfWeek = null;
        if (byDay != null && !byDay.isBlank()) {
            String trimmed = byDay.trim();
            if (trimmed.startsWith("-1")) {
                position = -1;
                dayOfWeek = parseSingleDay(trimmed.substring(2));
            } else {
                // Extract leading digit(s)
                int i = 0;
                while (i < trimmed.length() && (Character.isDigit(trimmed.charAt(i)) || trimmed.charAt(i) == '-')) {
                    i++;
                }
                if (i > 0 && i < trimmed.length()) {
                    position = Integer.parseInt(trimmed.substring(0, i));
                    dayOfWeek = parseSingleDay(trimmed.substring(i));
                }
            }
        }

        if (dayOfWeek == null) {
            // Fallback: same day of month
            LocalDate current = start;
            while (!current.isAfter(rangeEnd)) {
                if (!current.isBefore(rangeStart)) {
                    dates.add(current);
                }
                current = current.plusMonths(interval);
            }
            return dates;
        }

        LocalDate current = start.withDayOfMonth(1);
        int monthCounter = 0;
        final DayOfWeek dow = dayOfWeek;
        final int pos = position;

        while (!current.isAfter(rangeEnd)) {
            if (monthCounter % interval == 0) {
                LocalDate occurrence = getNthDayOfWeekInMonth(current, pos, dow);
                if (occurrence != null && !occurrence.isBefore(start) && !occurrence.isBefore(rangeStart) && !occurrence.isAfter(rangeEnd)) {
                    dates.add(occurrence);
                }
            }
            current = current.plusMonths(1);
            monthCounter++;
        }
        return dates;
    }

    private List<LocalDate> expandYearly(LocalDate start, LocalDate rangeStart, LocalDate rangeEnd,
                                          int interval, String byMonth, String byDay) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(rangeEnd)) {
            if (!current.isBefore(rangeStart)) {
                dates.add(current);
            }
            current = current.plusYears(interval);
        }
        return dates;
    }

    private LocalDate getNthDayOfWeekInMonth(LocalDate monthStart, int position, DayOfWeek dayOfWeek) {
        if (position == -1) {
            return monthStart.with(TemporalAdjusters.lastInMonth(dayOfWeek));
        }
        if (position >= 1 && position <= 5) {
            LocalDate first = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek));
            LocalDate result = first.plusWeeks(position - 1);
            if (result.getMonth() == monthStart.getMonth()) {
                return result;
            }
            return null; // 5th occurrence doesn't exist this month
        }
        return null;
    }

    private Set<DayOfWeek> parseDays(String byDay) {
        if (byDay == null || byDay.isBlank()) return Set.of();
        return Arrays.stream(byDay.split(","))
                .map(String::trim)
                .map(this::parseSingleDay)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private DayOfWeek parseSingleDay(String abbr) {
        return switch (abbr.toUpperCase()) {
            case "MO" -> DayOfWeek.MONDAY;
            case "TU" -> DayOfWeek.TUESDAY;
            case "WE" -> DayOfWeek.WEDNESDAY;
            case "TH" -> DayOfWeek.THURSDAY;
            case "FR" -> DayOfWeek.FRIDAY;
            case "SA" -> DayOfWeek.SATURDAY;
            case "SU" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
