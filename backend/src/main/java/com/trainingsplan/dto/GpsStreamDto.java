package com.trainingsplan.dto;

/**
 * GPS stream data for map visualization, containing only the fields needed for route display.
 * Arrays are aligned by index — each index represents one sample point.
 *
 * @param completedTrainingId the source activity ID
 * @param latlng              array of [lat, lng] pairs
 * @param distance            cumulative distance in meters at each sample
 * @param heartRate           heart rate in bpm per sample; null entries indicate dropouts
 * @param paceSecondsPerKm    pace in seconds/km per sample; null for stops
 * @param altitude            altitude in meters per sample; null when unavailable
 * @param hasHeartRate        true when HR data contains at least one non-null value
 * @param hasPace             true when pace data contains at least one non-null value
 * @param hasAltitude         true when altitude data contains at least one non-null value
 */
public record GpsStreamDto(
        Long completedTrainingId,
        double[][] latlng,
        double[] distance,
        Integer[] heartRate,
        Integer[] paceSecondsPerKm,
        Double[] altitude,
        boolean hasHeartRate,
        boolean hasPace,
        boolean hasAltitude
) {}
