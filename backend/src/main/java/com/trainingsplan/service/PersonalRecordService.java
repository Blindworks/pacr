package com.trainingsplan.service;

import com.trainingsplan.dto.AddPersonalRecordEntryRequest;
import com.trainingsplan.dto.CreatePersonalRecordRequest;
import com.trainingsplan.dto.PersonalRecordDto;
import com.trainingsplan.dto.PersonalRecordEntryDto;
import com.trainingsplan.dto.UpdateGoalRequest;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.PersonalRecord;
import com.trainingsplan.entity.PersonalRecordEntry;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.PersonalRecordEntryRepository;
import com.trainingsplan.repository.PersonalRecordRepository;
import com.trainingsplan.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@Service
public class PersonalRecordService {

    private static final Logger log = LoggerFactory.getLogger(PersonalRecordService.class);

    private static final double DISTANCE_TOLERANCE = 0.05;

    @Autowired
    private PersonalRecordRepository personalRecordRepository;

    @Autowired
    private PersonalRecordEntryRepository personalRecordEntryRepository;

    @Autowired
    private UserRepository userRepository;

    private static final List<double[]> DEFAULT_DISTANCES = Arrays.asList(
            new double[]{5.0},
            new double[]{10.0},
            new double[]{21.0975},
            new double[]{42.195}
    );
    private static final List<String> DEFAULT_LABELS = Arrays.asList("5K", "10K", "Halbmarathon", "Marathon");

    @Transactional
    public List<PersonalRecordDto> getAllForUser(Long userId) {
        List<PersonalRecord> records = personalRecordRepository.findByUserIdOrderByDistanceKmAsc(userId);
        if (records.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
            for (int i = 0; i < DEFAULT_DISTANCES.size(); i++) {
                PersonalRecord r = new PersonalRecord();
                r.setUser(user);
                r.setDistanceKm(DEFAULT_DISTANCES.get(i)[0]);
                r.setDistanceLabel(DEFAULT_LABELS.get(i));
                personalRecordRepository.save(r);
            }
            records = personalRecordRepository.findByUserIdOrderByDistanceKmAsc(userId);
        }
        return records.stream().map(this::toDto).toList();
    }

    @Transactional
    public PersonalRecordDto createRecord(Long userId, CreatePersonalRecordRequest request) {
        if (personalRecordRepository.findByUserIdAndDistanceKm(userId, request.getDistanceKm()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese Distanz existiert bereits");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        PersonalRecord record = new PersonalRecord();
        record.setUser(user);
        record.setDistanceKm(request.getDistanceKm());
        record.setDistanceLabel(request.getDistanceLabel());
        record.setGoalTimeSeconds(request.getGoalTimeSeconds());

        return toDto(personalRecordRepository.save(record));
    }

    @Transactional
    public PersonalRecordDto updateGoal(Long userId, Long recordId, UpdateGoalRequest request) {
        PersonalRecord record = loadOwnedRecord(userId, recordId);
        record.setGoalTimeSeconds(request.getGoalTimeSeconds());
        record.setUpdatedAt(LocalDateTime.now());
        return toDto(personalRecordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        PersonalRecord record = loadOwnedRecord(userId, recordId);
        personalRecordRepository.delete(record);
    }

    public List<PersonalRecordEntryDto> getEntries(Long userId, Long recordId) {
        loadOwnedRecord(userId, recordId);
        return personalRecordEntryRepository.findByPersonalRecordIdOrderByAchievedDateDesc(recordId)
                .stream()
                .map(this::toEntryDto)
                .toList();
    }

    @Transactional
    public PersonalRecordEntryDto addManualEntry(Long userId, Long recordId, AddPersonalRecordEntryRequest request) {
        PersonalRecord record = loadOwnedRecord(userId, recordId);

        PersonalRecordEntry entry = new PersonalRecordEntry();
        entry.setPersonalRecord(record);
        entry.setTimeSeconds(request.getTimeSeconds());
        entry.setAchievedDate(request.getAchievedDate());
        entry.setManual(true);

        record.setUpdatedAt(LocalDateTime.now());
        personalRecordRepository.save(record);

        return toEntryDto(personalRecordEntryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(Long userId, Long entryId) {
        PersonalRecordEntry entry = personalRecordEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found: " + entryId));

        if (!personalRecordEntryRepository.existsByIdAndUserId(entryId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        personalRecordEntryRepository.delete(entry);
    }

    @Transactional
    public void checkAndUpdateFromActivity(User user, CompletedTraining activity) {
        if (activity.getDistanceKm() == null) {
            return;
        }

        Integer activityTime = resolveActivityTime(activity);
        if (activityTime == null || activityTime <= 0) {
            return;
        }

        List<PersonalRecord> records = personalRecordRepository.findByUserIdOrderByDistanceKmAsc(user.getId());

        for (PersonalRecord record : records) {
            double deviation = Math.abs(activity.getDistanceKm() - record.getDistanceKm()) / record.getDistanceKm();
            if (deviation > DISTANCE_TOLERANCE) {
                continue;
            }

            Long activityId = activity.getId();
            if (activityId != null && personalRecordEntryRepository.existsByPersonalRecordIdAndCompletedTrainingId(record.getId(), activityId)) {
                continue;
            }

            PersonalRecordEntry entry = new PersonalRecordEntry();
            entry.setPersonalRecord(record);
            entry.setTimeSeconds(activityTime);
            entry.setAchievedDate(activity.getTrainingDate());
            entry.setManual(false);
            entry.setCompletedTrainingId(activityId);

            personalRecordEntryRepository.save(entry);

            record.setUpdatedAt(LocalDateTime.now());
            personalRecordRepository.save(record);

            log.info("personal_record_entry_added recordId={} userId={} activityId={} timeSeconds={}",
                    record.getId(), user.getId(), activity.getId(), activityTime);
        }
    }

    // --- private helpers ---

    private PersonalRecord loadOwnedRecord(Long userId, Long recordId) {
        PersonalRecord record = personalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: " + recordId));
        if (!record.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return record;
    }

    private Integer resolveActivityTime(CompletedTraining activity) {
        if (activity.getMovingTimeSeconds() != null && activity.getMovingTimeSeconds() > 0) {
            return activity.getMovingTimeSeconds();
        }
        return activity.getDurationSeconds();
    }

    private PersonalRecordDto toDto(PersonalRecord record) {
        PersonalRecordDto dto = new PersonalRecordDto();
        dto.setId(record.getId());
        dto.setDistanceKm(record.getDistanceKm());
        dto.setDistanceLabel(record.getDistanceLabel());
        dto.setGoalTimeSeconds(record.getGoalTimeSeconds());

        personalRecordEntryRepository.findTopByPersonalRecordIdOrderByTimeSecondsAsc(record.getId())
                .ifPresent(best -> {
                    dto.setBestTimeSeconds(best.getTimeSeconds());
                    dto.setAchievedDate(best.getAchievedDate());
                    dto.setIsManual(best.isManual());
                    dto.setActivityId(best.getCompletedTrainingId());
                });

        return dto;
    }

    private PersonalRecordEntryDto toEntryDto(PersonalRecordEntry entry) {
        PersonalRecordEntryDto dto = new PersonalRecordEntryDto();
        dto.setId(entry.getId());
        dto.setTimeSeconds(entry.getTimeSeconds());
        dto.setAchievedDate(entry.getAchievedDate());
        dto.setIsManual(entry.isManual());
        dto.setActivityId(entry.getCompletedTrainingId());
        return dto;
    }
}
