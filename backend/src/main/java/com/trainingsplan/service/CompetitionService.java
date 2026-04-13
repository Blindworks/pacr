package com.trainingsplan.service;

import com.trainingsplan.dto.CompetitionDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.CompetitionFormatRepository;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompetitionService {

    private static final Logger log = LoggerFactory.getLogger(CompetitionService.class);

    private final CompetitionRepository competitionRepository;
    private final CompetitionRegistrationRepository registrationRepository;
    private final CompetitionFormatRepository competitionFormatRepository;
    private final UserTrainingEntryRepository userTrainingEntryRepository;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public CompetitionService(CompetitionRepository competitionRepository,
                              CompetitionRegistrationRepository registrationRepository,
                              CompetitionFormatRepository competitionFormatRepository,
                              UserTrainingEntryRepository userTrainingEntryRepository,
                              SecurityUtils securityUtils,
                              AuditLogService auditLogService) {
        this.competitionRepository = competitionRepository;
        this.registrationRepository = registrationRepository;
        this.competitionFormatRepository = competitionFormatRepository;
        this.userTrainingEntryRepository = userTrainingEntryRepository;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    public List<CompetitionDto> findAll(boolean includeSystemGenerated) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("[CompetitionService] findAll() called, userId={}, includeSystemGenerated={}", userId, includeSystemGenerated);
        try {
            List<Competition> all = includeSystemGenerated
                    ? competitionRepository.findAll()
                    : competitionRepository.findBySystemGeneratedFalse();
            log.info("[CompetitionService] found {} competitions in DB", all.size());
            List<CompetitionDto> result = all.stream()
                    .map(c -> {
                        log.debug("[CompetitionService] mapping competition id={} name='{}'", c.getId(), c.getName());
                        CompetitionRegistration reg = userId != null
                                ? registrationRepository.findByCompetitionIdAndUserId(c.getId(), userId).orElse(null)
                                : null;
                        log.debug("[CompetitionService] registration for competition {}: {}", c.getId(), reg != null ? "found id=" + reg.getId() : "none");
                        return new CompetitionDto(c, reg);
                    })
                    .collect(Collectors.toList());
            log.info("[CompetitionService] findAll() returning {} DTOs", result.size());
            return result;
        } catch (Exception e) {
            log.error("[CompetitionService] findAll() failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public CompetitionDto findById(Long id) {
        Competition competition = competitionRepository.findById(id).orElse(null);
        if (competition == null) return null;
        Long userId = securityUtils.getCurrentUserId();
        CompetitionRegistration reg = userId != null
                ? registrationRepository.findByCompetitionIdAndUserId(id, userId).orElse(null)
                : null;
        return new CompetitionDto(competition, reg);
    }

    public Competition findEntityById(Long id) {
        return competitionRepository.findById(id).orElse(null);
    }

    @Transactional
    public CompetitionDto save(Competition competition) {
        boolean isNew = competition.getId() == null;

        // Handle formats: set parent reference for cascade persistence
        List<CompetitionFormat> incomingFormats = competition.getFormats();

        if (!isNew) {
            // For updates, load existing entity and merge formats
            Competition existing = competitionRepository.findById(competition.getId()).orElse(null);
            if (existing != null) {
                existing.setName(competition.getName());
                existing.setDate(competition.getDate());
                existing.setDescription(competition.getDescription());
                existing.setType(competition.getType());
                existing.setLocation(competition.getLocation());
                existing.setLatitude(competition.getLatitude());
                existing.setLongitude(competition.getLongitude());
                existing.setStartTime(competition.getStartTime());
                existing.setSystemGenerated(competition.isSystemGenerated());

                // Sync formats: clear and re-add to trigger orphanRemoval
                existing.getFormats().clear();
                if (incomingFormats != null) {
                    for (CompetitionFormat f : incomingFormats) {
                        f.setCompetition(existing);
                        existing.getFormats().add(f);
                    }
                }
                competition = existing;
            }
        } else {
            if (incomingFormats != null) {
                for (CompetitionFormat f : incomingFormats) {
                    f.setCompetition(competition);
                }
            }
        }

        Competition saved = competitionRepository.save(competition);
        Long userId = securityUtils.getCurrentUserId();
        CompetitionRegistration reg = userId != null
                ? registrationRepository.findByCompetitionIdAndUserId(saved.getId(), userId).orElse(null)
                : null;
        if (isNew) {
            User currentUser = securityUtils.getCurrentUser();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("name", saved.getName());
            if (saved.getDate() != null) details.put("date", saved.getDate().toString());
            if (saved.getLocation() != null) details.put("location", saved.getLocation());
            auditLogService.log(currentUser, AuditAction.COMPETITION_CREATED, "COMPETITION",
                    String.valueOf(saved.getId()), details);
        }
        return new CompetitionDto(saved, reg);
    }

    @Transactional
    public void deleteById(Long id) {
        String competitionName = competitionRepository.findById(id)
                .map(Competition::getName).orElse(null);

        // 1. Delete UserTrainingEntries linked via CompetitionRegistration
        userTrainingEntryRepository.deleteByCompetitionRegistration_Competition_Id(id);

        // 2. Delete all CompetitionRegistrations for this competition
        List<CompetitionRegistration> registrations = registrationRepository.findByCompetitionId(id);
        if (!registrations.isEmpty()) {
            registrationRepository.deleteAll(registrations);
        }

        // 3. Delete the competition itself
        competitionRepository.deleteById(id);

        User currentUser = securityUtils.getCurrentUser();
        Map<String, Object> details = new LinkedHashMap<>();
        if (competitionName != null) details.put("name", competitionName);
        auditLogService.log(currentUser, AuditAction.COMPETITION_DELETED, "COMPETITION",
                String.valueOf(id), details.isEmpty() ? null : details);
    }

    public CompetitionRegistration updateRegistration(Long competitionId, String ranking,
                                                      String targetTime, Boolean registeredWithOrganizer,
                                                      Long formatId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new RuntimeException("Not authenticated");
        CompetitionRegistration reg = registrationRepository
                .findByCompetitionIdAndUserId(competitionId, userId)
                .orElseThrow(() -> new RuntimeException("Not registered for competition: " + competitionId));
        if (ranking != null) reg.setRanking(ranking);
        if (targetTime != null) reg.setTargetTime(targetTime);
        if (registeredWithOrganizer != null) reg.setRegisteredWithOrganizer(registeredWithOrganizer);
        if (formatId != null) {
            CompetitionFormat format = competitionFormatRepository.findById(formatId)
                    .orElseThrow(() -> new RuntimeException("Competition format not found: " + formatId));
            reg.setCompetitionFormat(format);
        }
        return registrationRepository.save(reg);
    }

    public CompetitionRegistration register(Long competitionId, Long formatId, String targetTime,
                                            Boolean registeredWithOrganizer, String ranking) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));
        var user = securityUtils.getCurrentUser();
        if (user == null) throw new RuntimeException("Not authenticated");
        Optional<CompetitionRegistration> existing = registrationRepository
                .findByCompetitionIdAndUserId(competitionId, user.getId());
        if (existing.isPresent()) return existing.get();

        CompetitionRegistration reg = new CompetitionRegistration(competition, user);

        // Resolve competition format if provided
        if (formatId != null) {
            CompetitionFormat format = competitionFormatRepository.findById(formatId)
                    .orElseThrow(() -> new RuntimeException("Competition format not found: " + formatId));
            if (!format.getCompetition().getId().equals(competitionId)) {
                throw new RuntimeException("Format does not belong to this competition");
            }
            reg.setCompetitionFormat(format);
        } else if (competition.getFormats() != null && !competition.getFormats().isEmpty()) {
            throw new RuntimeException("Must select a format for this competition");
        }

        if (targetTime != null) reg.setTargetTime(targetTime);
        if (registeredWithOrganizer != null) reg.setRegisteredWithOrganizer(registeredWithOrganizer);
        if (ranking != null) reg.setRanking(ranking);
        return registrationRepository.save(reg);
    }

    public void unregister(Long competitionId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new RuntimeException("Not authenticated");
        registrationRepository.findByCompetitionIdAndUserId(competitionId, userId)
                .ifPresent(registrationRepository::delete);
    }
}
