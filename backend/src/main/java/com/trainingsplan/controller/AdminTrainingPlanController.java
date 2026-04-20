package com.trainingsplan.controller;

import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.service.TrainingPlanService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/training-plans")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public AdminTrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainingPlan(@PathVariable Long id) {
        if (trainingPlanService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        trainingPlanService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportTrainingPlan(@PathVariable Long id) throws Exception {
        TrainingPlan plan = trainingPlanService.findById(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        String json = trainingPlanService.exportAsJson(id);
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String slug = plan.getName() == null ? "plan"
                : plan.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) slug = "plan";
        String filename = "plan-" + id + "-" + slug + ".json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDisposition(org.springframework.http.ContentDisposition
                .attachment().filename(filename).build());
        headers.setAccessControlExposeHeaders(java.util.List.of("Content-Disposition"));

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
