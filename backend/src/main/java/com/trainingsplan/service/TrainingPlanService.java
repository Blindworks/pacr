package com.trainingsplan.service;

import com.trainingsplan.dto.TrainingPlanDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.TrainingPlanRepository;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingPlanService {

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private CompetitionRegistrationRepository registrationRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private UserTrainingScheduleService userTrainingScheduleService;

    @Autowired
    private AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Basic CRUD
    // -------------------------------------------------------------------------

    public List<TrainingPlan> findAll() {
        return trainingPlanRepository.findAll();
    }

    public TrainingPlan findById(Long id) {
        return trainingPlanRepository.findById(id).orElse(null);
    }

    public TrainingPlan save(TrainingPlan trainingPlan) {
        return trainingPlanRepository.save(trainingPlan);
    }

    public void deleteById(Long id) {
        trainingPlanRepository.deleteById(id);
        User currentUser = securityUtils.getCurrentUser();
        auditLogService.log(currentUser, AuditAction.PLAN_DELETED, "TRAINING_PLAN",
                String.valueOf(id), null);
    }

    // -------------------------------------------------------------------------
    // Upload plan directly linked to a competition
    // -------------------------------------------------------------------------

    @Transactional
    public TrainingPlan uploadTrainingPlan(MultipartFile file, String name, String description, Long competitionId) throws Exception {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));

        String jsonContent = new String(file.getBytes());
        TrainingPlan trainingPlan = new TrainingPlan(name, description, jsonContent);
        trainingPlan.setTrainingCount(countTrainingsInJson(jsonContent));
        TrainingPlan savedPlan = trainingPlanRepository.save(trainingPlan);

        User currentUser = securityUtils.getCurrentUser();
        auditLogService.log(currentUser, AuditAction.PLAN_CREATED, "TRAINING_PLAN",
                String.valueOf(savedPlan.getId()), null);

        // Create training templates
        parseAndCreateTemplates(savedPlan, jsonContent);

        // Update registration and generate user schedule
        CompetitionRegistration reg = updateRegistrationPlan(competition, savedPlan);
        if (reg != null) {
            userTrainingScheduleService.reassignPlan(reg);
        }

        return savedPlan;
    }

    // -------------------------------------------------------------------------
    // Template operations
    // -------------------------------------------------------------------------

    /**
     * Saves the plan JSON as a library template and creates Training template records
     * with weekNumber + dayOfWeek. No competition or user date needed.
     */
    @Transactional
    public TrainingPlanDto uploadAsTemplate(MultipartFile file, String name, String description) throws Exception {
        String jsonContent = new String(file.getBytes());
        objectMapper.readTree(jsonContent); // validate JSON

        TrainingPlan template = new TrainingPlan(name, description, jsonContent);
        template.setTrainingCount(countTrainingsInJson(jsonContent));
        TrainingPlan saved = trainingPlanRepository.save(template);

        User currentUser = securityUtils.getCurrentUser();
        auditLogService.log(currentUser, AuditAction.PLAN_CREATED, "TRAINING_PLAN",
                String.valueOf(saved.getId()), null);

        parseAndCreateTemplates(saved, jsonContent);

        return new TrainingPlanDto(saved);
    }

    /**
     * Returns all plans (all plans are implicitly templates in the new architecture).
     */
    public List<TrainingPlanDto> findAllTemplates() {
        return trainingPlanRepository.findAll()
                .stream()
                .map(TrainingPlanDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Assigns an existing plan to a competition. Ensures training templates exist,
     * then generates UserTrainingEntry records with computed dates.
     */
    @Transactional
    public TrainingPlanDto assignPlanToCompetition(Long planId, Long competitionId) throws Exception {
        TrainingPlan sourcePlan = trainingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Training plan not found: " + planId));
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));

        // Ensure templates exist (parse from stored JSON if needed)
        List<Training> existingTemplates = trainingRepository.findByTrainingPlan_Id(planId);
        if (existingTemplates.isEmpty() && sourcePlan.getJsonContent() != null) {
            parseAndCreateTemplates(sourcePlan, sourcePlan.getJsonContent());
        }

        // Update registration and generate user schedule
        CompetitionRegistration reg = updateRegistrationPlan(competition, sourcePlan);
        if (reg != null) {
            userTrainingScheduleService.reassignPlan(reg);
        }

        return new TrainingPlanDto(sourcePlan);
    }

    // -------------------------------------------------------------------------
    // Template creation (Training records with weekNumber + dayOfWeek)
    // -------------------------------------------------------------------------

    private void clearTemplatesForPlan(TrainingPlan plan) {
        List<Training> existing = trainingRepository.findByTrainingPlan_Id(plan.getId());
        if (!existing.isEmpty()) {
            trainingRepository.deleteAll(existing);
        }
    }

    private void parseAndCreateTemplates(TrainingPlan trainingPlan, String jsonContent) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            List<Training> templates = new ArrayList<>();

            if (rootNode.has("trainings") && rootNode.get("trainings").isArray()) {
                parseOldFormatAsTemplates(rootNode, templates, trainingPlan);
            } else if (rootNode.has("marathon_plan")) {
                parseMarathonPlanAsTemplates(rootNode.get("marathon_plan"), templates, trainingPlan);
            } else if (rootNode.has("half_marathon_plan")) {
                parseMarathonPlanAsTemplates(rootNode.get("half_marathon_plan"), templates, trainingPlan);
            } else if (rootNode.has("weeks")) {
                parseMarathonPlanAsTemplates(rootNode, templates, trainingPlan);
            }

            trainingRepository.saveAll(templates);

        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON training plan: " + e.getMessage(), e);
        }
    }

    /**
     * Old format: absolute dates in JSON → convert to weekNumber + dayOfWeek.
     * weekNumber=1 = first week, weekNumber=N = race week (highest number = closest to race).
     */
    private void parseOldFormatAsTemplates(JsonNode rootNode, List<Training> templates, TrainingPlan trainingPlan) {
        JsonNode trainingsNode = rootNode.get("trainings");

        LocalDate minDate = LocalDate.MAX;
        for (JsonNode t : trainingsNode) {
            try {
                LocalDate d = LocalDate.parse(t.path("date").asText());
                if (d.isBefore(minDate)) minDate = d;
            } catch (Exception ignored) {}
        }

        for (JsonNode trainingNode : trainingsNode) {
            try {
                LocalDate trainingDate = LocalDate.parse(trainingNode.path("date").asText());
                long daysDiff = ChronoUnit.DAYS.between(minDate, trainingDate);
                int weekNumber = (int) (daysDiff / 7) + 1;
                DayOfWeek dayOfWeek = trainingDate.getDayOfWeek();

                Training template = new Training();
                template.setName(trainingNode.path("name").asText("Training"));
                template.setDescription(trainingNode.path("description").asText(""));
                template.setWeekNumber(weekNumber);
                template.setDayOfWeek(dayOfWeek);
                template.setTrainingType(trainingNode.path("type").asText(""));
                template.setIntensityLevel(trainingNode.path("intensity").asText(""));
                if (trainingNode.has("duration")) {
                    template.setDurationMinutes(trainingNode.get("duration").asInt());
                }
                template.setTrainingPlan(trainingPlan);
                templates.add(template);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Marathon/half-marathon format: week schedules with week number field.
     * weekNumber is taken directly from JSON: week 1 = first week, week N = race week.
     */
    private void parseMarathonPlanAsTemplates(JsonNode planNode, List<Training> templates, TrainingPlan trainingPlan) {
        JsonNode weeksNode = planNode.get("weeks");
        if (weeksNode == null || !weeksNode.isArray()) return;

        String[] weekdayNames = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        DayOfWeek[] weekdays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        for (JsonNode weekNode : weeksNode) {
            int weekNumber = weekNode.get("week").asInt();
            JsonNode scheduleNode = weekNode.get("schedule");
            if (scheduleNode == null) continue;

            for (int dayIndex = 0; dayIndex < weekdayNames.length; dayIndex++) {
                JsonNode dayNode = scheduleNode.get(weekdayNames[dayIndex]);
                if (dayNode == null || !dayNode.has("workout")) continue;

                String workout = dayNode.get("workout").asText();
                String intensity = dayNode.has("intensity") ? dayNode.get("intensity").asText("") : "";
                if ("Ruhetag".equals(workout) || "0%".equals(intensity)) continue;

                Training template = new Training();
                template.setName(workout);
                template.setDescription(workout);
                template.setWeekNumber(weekNumber);
                template.setDayOfWeek(weekdays[dayIndex]);
                template.setTrainingType(extractTrainingType(workout));
                template.setIntensityLevel(mapIntensityLevel(intensity));
                template.setTrainingPlan(trainingPlan);
                Integer duration = extractDuration(workout);
                if (duration != null) template.setDurationMinutes(duration);
                templates.add(template);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private CompetitionRegistration updateRegistrationPlan(Competition competition, TrainingPlan plan) {
        var user = securityUtils.getCurrentUser();
        if (user == null) return null;
        CompetitionRegistration reg = registrationRepository
                .findByCompetitionIdAndUserId(competition.getId(), user.getId())
                .orElseGet(() -> {
                    CompetitionRegistration newReg = new CompetitionRegistration(competition, user);
                    return registrationRepository.save(newReg);
                });
        reg.setTrainingPlan(plan);
        return registrationRepository.save(reg);
    }

    private Integer countTrainingsInJson(String jsonContent) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (root.has("trainings") && root.get("trainings").isArray()) {
                return root.get("trainings").size();
            }
            JsonNode planNode = root.has("marathon_plan") ? root.get("marathon_plan")
                    : root.has("half_marathon_plan") ? root.get("half_marathon_plan")
                    : root.has("weeks") ? root : null;

            if (planNode != null && planNode.has("weeks")) {
                int count = 0;
                String[] weekdays = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
                for (JsonNode weekNode : planNode.get("weeks")) {
                    JsonNode schedule = weekNode.get("schedule");
                    if (schedule == null) continue;
                    for (String day : weekdays) {
                        JsonNode dayNode = schedule.get(day);
                        if (dayNode != null && dayNode.has("workout")) {
                            String workout = dayNode.get("workout").asText("");
                            String intensity = dayNode.has("intensity") ? dayNode.get("intensity").asText("") : "";
                            if (!"Ruhetag".equals(workout) && !"0%".equals(intensity)) {
                                count++;
                            }
                        }
                    }
                }
                return count;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractTrainingType(String workout) {
        String w = workout.toLowerCase();
        if (w.contains("krafttraining")) return "strength";
        if (w.contains("intervall")) return "interval";
        if (w.contains("wettkampf")) return "race";
        if (w.contains("fahrtspiel")) return "fartlek";
        if (w.contains("schwimmen")) return "swimming";
        if (w.contains("radfahren")) return "cycling";
        if (w.contains("dauerlauf") || w.contains("km")) return "endurance";
        return "general";
    }

    private String mapIntensityLevel(String intensityPercent) {
        if (intensityPercent == null || intensityPercent.isEmpty() || "0%".equals(intensityPercent)) return "rest";
        try {
            int intensity = Integer.parseInt(intensityPercent.replace("%", ""));
            if (intensity >= 90) return "high";
            if (intensity >= 75) return "medium";
            if (intensity >= 65) return "low";
            return "recovery";
        } catch (NumberFormatException e) {
            return "medium";
        }
    }

    private Integer extractDuration(String workout) {
        if (workout.contains("1 h")) return 60;
        if (workout.contains("2 h")) return 120;
        if (workout.contains("1,5 h")) return 90;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*km");
        java.util.regex.Matcher matcher = pattern.matcher(workout);
        if (matcher.find()) {
            int km = Integer.parseInt(matcher.group(1));
            return km * 6;
        }
        return null;
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
