package com.trainingsplan.controller;

import com.trainingsplan.dto.AddPersonalRecordEntryRequest;
import com.trainingsplan.dto.CreatePersonalRecordRequest;
import com.trainingsplan.dto.PersonalRecordDto;
import com.trainingsplan.dto.PersonalRecordEntryDto;
import com.trainingsplan.dto.UpdateGoalRequest;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.PersonalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/personal-records")
public class PersonalRecordController {

    @Autowired
    private PersonalRecordService personalRecordService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<PersonalRecordDto>> getAllForUser() {
        Long userId = requireCurrentUserId();
        return ResponseEntity.ok(personalRecordService.getAllForUser(userId));
    }

    @PostMapping
    public ResponseEntity<PersonalRecordDto> createRecord(@RequestBody CreatePersonalRecordRequest request) {
        Long userId = requireCurrentUserId();
        PersonalRecordDto created = personalRecordService.createRecord(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonalRecordDto> updateGoal(@PathVariable Long id,
                                                        @RequestBody UpdateGoalRequest request) {
        Long userId = requireCurrentUserId();
        return ResponseEntity.ok(personalRecordService.updateGoal(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        Long userId = requireCurrentUserId();
        personalRecordService.deleteRecord(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/entries")
    public ResponseEntity<List<PersonalRecordEntryDto>> getEntries(@PathVariable Long id) {
        Long userId = requireCurrentUserId();
        return ResponseEntity.ok(personalRecordService.getEntries(userId, id));
    }

    @PostMapping("/{id}/entries")
    public ResponseEntity<PersonalRecordEntryDto> addManualEntry(@PathVariable Long id,
                                                                 @RequestBody AddPersonalRecordEntryRequest request) {
        Long userId = requireCurrentUserId();
        PersonalRecordEntryDto created = personalRecordService.addManualEntry(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id, @PathVariable Long entryId) {
        Long userId = requireCurrentUserId();
        personalRecordService.deleteEntry(userId, entryId);
        return ResponseEntity.noContent().build();
    }

    private Long requireCurrentUserId() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return user.getId();
    }
}
