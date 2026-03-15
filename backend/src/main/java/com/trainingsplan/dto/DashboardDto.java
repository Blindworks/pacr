package com.trainingsplan.dto;

import java.time.LocalDate;
import java.util.List;

public class DashboardDto {

    private final double strain21;
    private final int readinessScore;
    private final String readinessRecommendation;
    private final List<String> readinessReasons;
    private final LoadStatusDto loadStatus;
    private final List<LoadTrendPointDto> loadTrend;
    private final List<EfTrendPointDto> efTrend;
    private final List<DriftTrendPointDto> driftTrend;
    private final LastRunDto lastRun;
    private final NextCompetitionDto nextCompetition;
    private final List<TrainingProgressDto> trainingProgress;
    private final Integer bodyBattery;

    public DashboardDto(
            double strain21,
            int readinessScore,
            String readinessRecommendation,
            List<String> readinessReasons,
            LoadStatusDto loadStatus,
            List<LoadTrendPointDto> loadTrend,
            List<EfTrendPointDto> efTrend,
            List<DriftTrendPointDto> driftTrend,
            LastRunDto lastRun,
            NextCompetitionDto nextCompetition,
            List<TrainingProgressDto> trainingProgress,
            Integer bodyBattery
    ) {
        this.strain21 = strain21;
        this.readinessScore = readinessScore;
        this.readinessRecommendation = readinessRecommendation;
        this.readinessReasons = readinessReasons;
        this.loadStatus = loadStatus;
        this.loadTrend = loadTrend;
        this.efTrend = efTrend;
        this.driftTrend = driftTrend;
        this.lastRun = lastRun;
        this.nextCompetition = nextCompetition;
        this.trainingProgress = trainingProgress;
        this.bodyBattery = bodyBattery;
    }

    public double getStrain21() {
        return strain21;
    }

    public int getReadinessScore() {
        return readinessScore;
    }

    public String getReadinessRecommendation() {
        return readinessRecommendation;
    }

    public List<String> getReadinessReasons() {
        return readinessReasons;
    }

    public LoadStatusDto getLoadStatus() {
        return loadStatus;
    }

    public List<LoadTrendPointDto> getLoadTrend() {
        return loadTrend;
    }

    public List<EfTrendPointDto> getEfTrend() {
        return efTrend;
    }

    public List<DriftTrendPointDto> getDriftTrend() {
        return driftTrend;
    }

    public LastRunDto getLastRun() {
        return lastRun;
    }

    public NextCompetitionDto getNextCompetition() {
        return nextCompetition;
    }

    public List<TrainingProgressDto> getTrainingProgress() {
        return trainingProgress;
    }

    public Integer getBodyBattery() {
        return bodyBattery;
    }

    public static class LoadStatusDto {
        private final double acwr;
        private final String flag;

        public LoadStatusDto(double acwr, String flag) {
            this.acwr = acwr;
            this.flag = flag;
        }

        public double getAcwr() {
            return acwr;
        }

        public String getFlag() {
            return flag;
        }
    }

    public static class LoadTrendPointDto {
        private final LocalDate date;
        private final double strain21;

        public LoadTrendPointDto(LocalDate date, double strain21) {
            this.date = date;
            this.strain21 = strain21;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getStrain21() {
            return strain21;
        }
    }

    public static class EfTrendPointDto {
        private final LocalDate date;
        private final double ef;

        public EfTrendPointDto(LocalDate date, double ef) {
            this.date = date;
            this.ef = ef;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getEf() {
            return ef;
        }
    }

    public static class DriftTrendPointDto {
        private final LocalDate date;
        private final double driftPct;

        public DriftTrendPointDto(LocalDate date, double driftPct) {
            this.date = date;
            this.driftPct = driftPct;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getDriftPct() {
            return driftPct;
        }
    }

    public static class LastRunDto {
        private final LocalDate date;
        private final double strain21;
        private final double driftPct;
        private final double z4Min;
        private final double z5Min;
        private final List<String> coachBullets;

        public LastRunDto(
                LocalDate date,
                double strain21,
                double driftPct,
                double z4Min,
                double z5Min,
                List<String> coachBullets
        ) {
            this.date = date;
            this.strain21 = strain21;
            this.driftPct = driftPct;
            this.z4Min = z4Min;
            this.z5Min = z5Min;
            this.coachBullets = coachBullets;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getStrain21() {
            return strain21;
        }

        public double getDriftPct() {
            return driftPct;
        }

        public double getZ4Min() {
            return z4Min;
        }

        public double getZ5Min() {
            return z5Min;
        }

        public List<String> getCoachBullets() {
            return coachBullets;
        }
    }

    public static class NextCompetitionDto {
        private final String competitionName;
        private final String competitionLocation;
        private final java.time.LocalDate date;
        private final int daysUntil;
        private final double elapsedPct;

        public NextCompetitionDto(String competitionName, String competitionLocation, java.time.LocalDate date, int daysUntil, double elapsedPct) {
            this.competitionName = competitionName;
            this.competitionLocation = competitionLocation;
            this.date = date;
            this.daysUntil = daysUntil;
            this.elapsedPct = elapsedPct;
        }

        public String getCompetitionName() { return competitionName; }
        public String getCompetitionLocation() { return competitionLocation; }
        public java.time.LocalDate getDate() { return date; }
        public int getDaysUntil() { return daysUntil; }
        public double getElapsedPct() { return elapsedPct; }
    }

    public static class TrainingProgressDto {
        private final long competitionId;
        private final String competitionName;
        private final java.time.LocalDate competitionDate;
        private final int total;
        private final int completed;

        public TrainingProgressDto(long competitionId, String competitionName, java.time.LocalDate competitionDate, int total, int completed) {
            this.competitionId = competitionId;
            this.competitionName = competitionName;
            this.competitionDate = competitionDate;
            this.total = total;
            this.completed = completed;
        }

        public long getCompetitionId() { return competitionId; }
        public String getCompetitionName() { return competitionName; }
        public java.time.LocalDate getCompetitionDate() { return competitionDate; }
        public int getTotal() { return total; }
        public int getCompleted() { return completed; }

        public double getCompletionPct() {
            return total > 0 ? Math.min(100.0, completed * 100.0 / total) : 0.0;
        }
    }
}
