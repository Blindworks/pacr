package com.trainingsplan.dto;

/**
 * Downsampled stream data for a completed training activity, ready for charting.
 * Arrays are aligned by index — each index represents one sample point.
 *
 * @param completedTrainingId the source activity ID
 * @param distancePoints      distance in km at each sample (x-axis for all charts)
 * @param heartRate           heart rate in bpm per sample; null entries indicate HR dropouts
 * @param altitude            altitude in meters per sample; null when stream unavailable
 * @param paceSecondsPerKm    pace in seconds/km per sample (1000 / velocity_m_s); null for stops
 * @param hasHeartRate        true when HR stream data is present
 * @param hasAltitude         true when altitude stream data is present
 * @param hasPace             true when velocity_smooth stream data is present
 */
public record ActivityStreamDto(
        Long completedTrainingId,
        double[] distancePoints,
        Integer[] heartRate,
        Double[] altitude,
        Integer[] paceSecondsPerKm,
        boolean hasHeartRate,
        boolean hasAltitude,
        boolean hasPace
) {}
