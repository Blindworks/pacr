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
 * @param cadence             cadence in spm/rpm per sample; null entries where data absent
 * @param power               power in watts per sample; null entries where data absent
 * @param hasHeartRate        true when HR stream contains at least one non-null value
 * @param hasAltitude         true when altitude stream contains at least one non-null value
 * @param hasPace             true when velocity stream contains at least one non-null value
 * @param hasCadence          true when cadence stream contains at least one non-null value
 * @param hasPower            true when power stream contains at least one non-null value
 */
public record ActivityStreamDto(
        Long completedTrainingId,
        double[] distancePoints,
        Integer[] heartRate,
        Double[] altitude,
        Integer[] paceSecondsPerKm,
        Integer[] cadence,
        Integer[] power,
        boolean hasHeartRate,
        boolean hasAltitude,
        boolean hasPace,
        boolean hasCadence,
        boolean hasPower
) {}
