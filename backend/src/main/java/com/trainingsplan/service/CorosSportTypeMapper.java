package com.trainingsplan.service;

import java.util.Map;

/**
 * Maps COROS mode/subMode pairs to human-readable sport names.
 * Based on COROS API Reference V2.0.6.
 */
public final class CorosSportTypeMapper {

    private CorosSportTypeMapper() {}

    private static final Map<String, String> SPORT_MAP = Map.ofEntries(
            // Running
            Map.entry("8_1", "Outdoor Run"),
            Map.entry("8_2", "Indoor Run"),
            Map.entry("20_1", "Track Run"),
            Map.entry("15_1", "Trail Run"),
            // Cycling
            Map.entry("9_1", "Outdoor Bike"),
            Map.entry("9_2", "Indoor Bike"),
            Map.entry("9_3", "E-Bike"),
            Map.entry("9_4", "Mountain Bike"),
            Map.entry("9_5", "E-Mountain Bike"),
            Map.entry("9_6", "Gravel Bike"),
            // Swimming
            Map.entry("10_1", "Open Water"),
            Map.entry("10_2", "Pool Swim"),
            // Multisport
            Map.entry("13_1", "Triathlon"),
            Map.entry("13_2", "Multisport"),
            Map.entry("13_3", "Ski Touring"),
            Map.entry("13_4", "Outdoor Climb"),
            // Mountain / Hiking
            Map.entry("14_1", "Mountain Climb"),
            Map.entry("16_1", "Hike"),
            Map.entry("31_1", "Walk"),
            // Cardio
            Map.entry("18_1", "GPS Cardio"),
            Map.entry("18_2", "Gym Cardio"),
            // Winter sports
            Map.entry("19_1", "XC Ski"),
            Map.entry("21_1", "Ski"),
            Map.entry("21_2", "Snowboard"),
            Map.entry("29_1", "Ski Touring"),
            // Other
            Map.entry("22_1", "Pilot"),
            Map.entry("23_2", "Strength"),
            // Water sports
            Map.entry("24_1", "Rowing"),
            Map.entry("24_2", "Indoor Rower"),
            Map.entry("25_1", "Whitewater"),
            Map.entry("26_1", "Flatwater"),
            Map.entry("27_1", "Windsurfing"),
            Map.entry("28_1", "Speedsurfing")
    );

    // Additional entries that exceed Map.ofEntries limit (10 entries per call for Map.of)
    private static final Map<String, String> SPORT_MAP_EXT = Map.ofEntries(
            // Fishing
            Map.entry("32_1", "Boat Fishing"),
            Map.entry("32_2", "Shore Fishing"),
            Map.entry("32_4", "Kayak Fishing"),
            Map.entry("32_5", "InShore Fishing"),
            Map.entry("32_6", "OffShore Fishing"),
            Map.entry("32_7", "Boat Fly Fishing"),
            Map.entry("32_8", "Shore Fly Fishing"),
            Map.entry("32_9", "Surf Fishing"),
            // Climbing
            Map.entry("33_2", "Single-Pitch"),
            Map.entry("33_3", "Bouldering"),
            // Ball & Racket
            Map.entry("34_2", "Jump Rope"),
            Map.entry("36_2", "Badminton"),
            Map.entry("37_2", "Table Tennis"),
            Map.entry("38_1", "Basketball"),
            Map.entry("39_1", "Soccer"),
            Map.entry("40_1", "Pickleball"),
            Map.entry("47_1", "Tennis"),
            // Fitness
            Map.entry("41_2", "Elliptical"),
            Map.entry("42_2", "Yoga"),
            Map.entry("43_2", "Pilates"),
            Map.entry("44_2", "Boxing"),
            // Other
            Map.entry("45_1", "Frisbee"),
            Map.entry("46_1", "Skateboard"),
            // Custom
            Map.entry("98_1", "Custom Sport Outdoor"),
            Map.entry("99_2", "Custom Sport Indoor")
    );

    /**
     * Returns the human-readable sport name for a COROS mode/subMode pair.
     */
    public static String getSportName(Integer mode, Integer subMode) {
        if (mode == null || subMode == null) {
            return "Unknown";
        }
        String key = mode + "_" + subMode;
        String name = SPORT_MAP.get(key);
        if (name != null) return name;
        name = SPORT_MAP_EXT.get(key);
        if (name != null) return name;
        return "Unknown (" + mode + "/" + subMode + ")";
    }

    /**
     * Returns a parent sport category based on the mode.
     * Used for the subSport field in CompletedTraining.
     */
    public static String getParentSport(Integer mode) {
        if (mode == null) return null;
        return switch (mode) {
            case 8, 15, 20 -> "Run";
            case 9 -> "Bike";
            case 10 -> "Swim";
            case 13 -> "Multisport";
            case 14, 16 -> "Hike";
            case 18 -> "Cardio";
            case 19, 21, 29 -> "Winter Sport";
            case 23 -> "Strength";
            case 24, 25, 26, 27, 28 -> "Water Sport";
            case 31 -> "Walk";
            case 32 -> "Fishing";
            case 33 -> "Climbing";
            case 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47 -> "Sport";
            default -> null;
        };
    }
}
