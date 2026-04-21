package com.trainingsplan.dto;

import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionFormat;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminRegistrationDto {

    private Long id;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private Long competitionId;
    private String competitionName;
    private LocalDate competitionDate;
    private Long trainingPlanId;
    private String trainingPlanName;
    private Long competitionFormatId;
    private String competitionFormatType;
    private LocalDateTime registeredAt;

    public AdminRegistrationDto() {}

    public static AdminRegistrationDto from(CompetitionRegistration reg) {
        AdminRegistrationDto dto = new AdminRegistrationDto();
        dto.id = reg.getId();

        User user = reg.getUser();
        if (user != null) {
            dto.userId = user.getId();
            dto.userEmail = user.getEmail();
            String first = user.getFirstName();
            String last = user.getLastName();
            String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            dto.userDisplayName = name.isEmpty() ? user.getUsername() : name;
        }

        Competition competition = reg.getCompetition();
        if (competition != null) {
            dto.competitionId = competition.getId();
            dto.competitionName = competition.getName();
            dto.competitionDate = competition.getDate();
        }

        TrainingPlan plan = reg.getTrainingPlan();
        if (plan != null) {
            dto.trainingPlanId = plan.getId();
            dto.trainingPlanName = plan.getName();
        }

        CompetitionFormat format = reg.getCompetitionFormat();
        if (format != null) {
            dto.competitionFormatId = format.getId();
            dto.competitionFormatType = format.getType() != null ? format.getType().name() : null;
        }

        dto.registeredAt = reg.getRegisteredAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getUserDisplayName() { return userDisplayName; }
    public Long getCompetitionId() { return competitionId; }
    public String getCompetitionName() { return competitionName; }
    public LocalDate getCompetitionDate() { return competitionDate; }
    public Long getTrainingPlanId() { return trainingPlanId; }
    public String getTrainingPlanName() { return trainingPlanName; }
    public Long getCompetitionFormatId() { return competitionFormatId; }
    public String getCompetitionFormatType() { return competitionFormatType; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
