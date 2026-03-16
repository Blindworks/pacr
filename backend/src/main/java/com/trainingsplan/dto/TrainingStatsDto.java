package com.trainingsplan.dto;

import java.util.List;

public class TrainingStatsDto {

    private List<Bucket> buckets;
    private double totalDistanceKm;
    private int totalDurationSeconds;
    private int totalActivityCount;
    private double avgPaceSecondsPerKm;
    private long totalZone1Seconds;
    private long totalZone2Seconds;
    private long totalZone3Seconds;
    private long totalZone4Seconds;
    private long totalZone5Seconds;

    public TrainingStatsDto() {}

    public TrainingStatsDto(List<Bucket> buckets, double totalDistanceKm, int totalDurationSeconds, int totalActivityCount) {
        this.buckets = buckets;
        this.totalDistanceKm = totalDistanceKm;
        this.totalDurationSeconds = totalDurationSeconds;
        this.totalActivityCount = totalActivityCount;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<Bucket> buckets) {
        this.buckets = buckets;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(int totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public int getTotalActivityCount() {
        return totalActivityCount;
    }

    public void setTotalActivityCount(int totalActivityCount) {
        this.totalActivityCount = totalActivityCount;
    }

    public double getAvgPaceSecondsPerKm() {
        return avgPaceSecondsPerKm;
    }

    public void setAvgPaceSecondsPerKm(double avgPaceSecondsPerKm) {
        this.avgPaceSecondsPerKm = avgPaceSecondsPerKm;
    }

    public long getTotalZone1Seconds() {
        return totalZone1Seconds;
    }

    public void setTotalZone1Seconds(long totalZone1Seconds) {
        this.totalZone1Seconds = totalZone1Seconds;
    }

    public long getTotalZone2Seconds() {
        return totalZone2Seconds;
    }

    public void setTotalZone2Seconds(long totalZone2Seconds) {
        this.totalZone2Seconds = totalZone2Seconds;
    }

    public long getTotalZone3Seconds() {
        return totalZone3Seconds;
    }

    public void setTotalZone3Seconds(long totalZone3Seconds) {
        this.totalZone3Seconds = totalZone3Seconds;
    }

    public long getTotalZone4Seconds() {
        return totalZone4Seconds;
    }

    public void setTotalZone4Seconds(long totalZone4Seconds) {
        this.totalZone4Seconds = totalZone4Seconds;
    }

    public long getTotalZone5Seconds() {
        return totalZone5Seconds;
    }

    public void setTotalZone5Seconds(long totalZone5Seconds) {
        this.totalZone5Seconds = totalZone5Seconds;
    }

    // -------------------------------------------------------------------------

    public static class Bucket {

        private String label;
        private String startDate;
        private String endDate;
        private double distanceKm;
        private int durationSeconds;
        private int elevationGainM;
        private int activityCount;

        public Bucket() {}

        public Bucket(String label, String startDate, String endDate,
                      double distanceKm, int durationSeconds, int elevationGainM, int activityCount) {
            this.label = label;
            this.startDate = startDate;
            this.endDate = endDate;
            this.distanceKm = distanceKm;
            this.durationSeconds = durationSeconds;
            this.elevationGainM = elevationGainM;
            this.activityCount = activityCount;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public int getElevationGainM() {
            return elevationGainM;
        }

        public void setElevationGainM(int elevationGainM) {
            this.elevationGainM = elevationGainM;
        }

        public int getActivityCount() {
            return activityCount;
        }

        public void setActivityCount(int activityCount) {
            this.activityCount = activityCount;
        }
    }
}
