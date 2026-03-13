package com.trainingsplan.service;

import com.trainingsplan.entity.BodyMeasurement;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.BodyMeasurementRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BodyMeasurementService {

    private static final Logger log = LoggerFactory.getLogger(BodyMeasurementService.class);

    @Autowired
    private BodyMeasurementRepository bodyMeasurementRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<BodyMeasurement> getAllForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.info("bodyMeasurements.getAll: userId=null, repository=findByUserIdOrderByMeasuredAtDesc, found=0");
            return Collections.emptyList();
        }

        List<BodyMeasurement> measurements = bodyMeasurementRepository.findByUserIdOrderByMeasuredAtDesc(userId);
        log.info(
                "bodyMeasurements.getAll: userId={}, repository=findByUserIdOrderByMeasuredAtDesc, sort=measuredAtDesc, found={}, ids={}, latestValues={}",
                userId,
                measurements.size(),
                measurements.stream().map(BodyMeasurement::getId).toList(),
                measurements.isEmpty() ? null : summarize(measurements.getFirst())
        );
        return measurements;
    }

    public Optional<BodyMeasurement> getLatestForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.info("bodyMeasurements.getLatest: userId=null, repository=findTopByUserIdOrderByMeasuredAtDesc, found=0");
            return Optional.empty();
        }

        Optional<BodyMeasurement> latest = bodyMeasurementRepository.findTopByUserIdOrderByMeasuredAtDesc(userId);
        log.info(
                "bodyMeasurements.getLatest: userId={}, repository=findTopByUserIdOrderByMeasuredAtDesc, sort=measuredAtDesc, found={}, measurementId={}, measuredAt={}, values={}",
                userId,
                latest.isPresent() ? 1 : 0,
                latest.map(BodyMeasurement::getId).orElse(null),
                latest.map(BodyMeasurement::getMeasuredAt).orElse(null),
                latest.map(this::summarize).orElse(null)
        );
        return latest;
    }

    private String summarize(BodyMeasurement measurement) {
        return String.format(
                "weightKg=%s,fatPercentage=%s,muscleMassKg=%s,bmi=%s,waterPercentage=%s,boneMassKg=%s,visceralFatLevel=%s",
                measurement.getWeightKg(),
                measurement.getFatPercentage(),
                measurement.getMuscleMassKg(),
                measurement.getBmi(),
                measurement.getWaterPercentage(),
                measurement.getBoneMassKg(),
                measurement.getVisceralFatLevel()
        );
    }

    @Transactional
    public BodyMeasurement create(BodyMeasurement measurement) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        measurement.setUser(user);
        return bodyMeasurementRepository.save(measurement);
    }

    @Transactional
    public BodyMeasurement update(Long id, BodyMeasurement updated) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        BodyMeasurement existing = bodyMeasurementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Measurement not found: " + id));

        existing.setMeasuredAt(updated.getMeasuredAt());
        existing.setWeightKg(updated.getWeightKg());
        existing.setFatPercentage(updated.getFatPercentage());
        existing.setWaterPercentage(updated.getWaterPercentage());
        existing.setMuscleMassKg(updated.getMuscleMassKg());
        existing.setBoneMassKg(updated.getBoneMassKg());
        existing.setVisceralFatLevel(updated.getVisceralFatLevel());
        existing.setMetabolicAge(updated.getMetabolicAge());
        existing.setBmi(updated.getBmi());
        existing.setNotes(updated.getNotes());

        return bodyMeasurementRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        BodyMeasurement measurement = bodyMeasurementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Measurement not found: " + id));
        bodyMeasurementRepository.delete(measurement);
    }
}
