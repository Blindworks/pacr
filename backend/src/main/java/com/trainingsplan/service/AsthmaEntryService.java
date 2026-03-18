package com.trainingsplan.service;

import com.trainingsplan.entity.AsthmaEntry;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.AsthmaEntryRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AsthmaEntryService {

    @Autowired
    private AsthmaEntryRepository repository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<AsthmaEntry> getAllForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        return repository.findByUserIdOrderByLoggedAtDesc(userId);
    }

    public List<AsthmaEntry> getLast7DaysForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return repository.findByUserIdAndLoggedAtAfterOrderByLoggedAtDesc(userId, sevenDaysAgo);
    }

    public AsthmaEntry create(AsthmaEntry entry) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        entry.setUser(user);
        if (entry.getLoggedAt() == null) {
            entry.setLoggedAt(LocalDateTime.now());
        }
        if (entry.getInhalerUsage() == null) {
            entry.setInhalerUsage("NONE");
        }
        return repository.save(entry);
    }

    public AsthmaEntry update(Long id, AsthmaEntry updated) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AsthmaEntry existing = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found or access denied"));
        existing.setSymptoms(updated.getSymptoms());
        existing.setSeverityScore(updated.getSeverityScore());
        existing.setPeakFlowLMin(updated.getPeakFlowLMin());
        existing.setInhalerUsage(updated.getInhalerUsage());
        existing.setNotes(updated.getNotes());
        if (updated.getLoggedAt() != null) {
            existing.setLoggedAt(updated.getLoggedAt());
        }
        return repository.save(existing);
    }

    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AsthmaEntry existing = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found or access denied"));
        repository.delete(existing);
    }
}
