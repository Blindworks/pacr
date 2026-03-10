package com.trainingsplan.service;

import com.trainingsplan.entity.CycleEntry;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CycleEntryRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CycleEntryService {

    @Autowired
    private CycleEntryRepository cycleEntryRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<CycleEntry> getAllForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        return cycleEntryRepository.findByUserIdOrderByEntryDateDesc(userId);
    }

    public Optional<CycleEntry> getLatestForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return cycleEntryRepository.findTopByUserIdOrderByEntryDateDesc(userId);
    }

    public Optional<CycleEntry> getByDateForCurrentUser(LocalDate date) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return cycleEntryRepository.findByUserIdAndEntryDate(userId, date);
    }

    @Transactional
    public CycleEntry create(CycleEntry entry) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        entry.setUser(user);
        return cycleEntryRepository.save(entry);
    }

    @Transactional
    public CycleEntry update(Long id, CycleEntry updated) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        CycleEntry existing = cycleEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cycle entry not found: " + id));

        existing.setEntryDate(updated.getEntryDate());
        existing.setPhysicalSymptoms(updated.getPhysicalSymptoms());
        existing.setMood(updated.getMood());
        existing.setEnergyLevel(updated.getEnergyLevel());
        existing.setSleepHours(updated.getSleepHours());
        existing.setSleepMinutes(updated.getSleepMinutes());
        existing.setSleepQuality(updated.getSleepQuality());
        existing.setFlowIntensity(updated.getFlowIntensity());
        existing.setNotes(updated.getNotes());

        return cycleEntryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        CycleEntry entry = cycleEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cycle entry not found: " + id));
        cycleEntryRepository.delete(entry);
    }
}
