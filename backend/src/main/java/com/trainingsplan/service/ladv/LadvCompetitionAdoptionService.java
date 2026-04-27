package com.trainingsplan.service.ladv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ladv.LadvLaufstrecke;
import com.trainingsplan.dto.ladv.LadvStadionfernItem;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionFormat;
import com.trainingsplan.entity.CompetitionType;
import com.trainingsplan.entity.LadvStagedEvent;
import com.trainingsplan.entity.LadvStagedEventStatus;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.LadvStagedEventRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a {@link LadvStagedEvent} into a real PACR {@link Competition} on
 * admin demand. Re-uses {@link CompetitionRepository#save} directly (rather
 * than {@code CompetitionService.save}) to avoid the user-scoped registration
 * lookup the latter does — adoption is system work.
 */
@Service
public class LadvCompetitionAdoptionService {

    private static final Logger log = LoggerFactory.getLogger(LadvCompetitionAdoptionService.class);

    private final LadvStagedEventRepository stagedRepo;
    private final CompetitionRepository competitionRepo;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    public LadvCompetitionAdoptionService(LadvStagedEventRepository stagedRepo,
                                          CompetitionRepository competitionRepo,
                                          ObjectMapper objectMapper,
                                          AuditLogService auditLogService,
                                          SecurityUtils securityUtils) {
        this.stagedRepo = stagedRepo;
        this.competitionRepo = competitionRepo;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public Competition adopt(Long stagedId) {
        LadvStagedEvent staged = stagedRepo.findById(stagedId)
                .orElseThrow(() -> new IllegalArgumentException("Staged event not found: " + stagedId));
        if (staged.getStatus() == LadvStagedEventStatus.IMPORTED) {
            throw new IllegalStateException("Event already imported as competition "
                    + (staged.getImportedCompetition() != null
                            ? staged.getImportedCompetition().getId() : "?"));
        }

        Competition competition = buildCompetition(staged);
        Competition saved = competitionRepo.save(competition);

        User actor = securityUtils.getCurrentUser();
        staged.setStatus(LadvStagedEventStatus.IMPORTED);
        staged.setImportedCompetition(saved);
        staged.setImportedAt(LocalDateTime.now());
        staged.setImportedBy(actor);
        stagedRepo.save(staged);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ladvId", staged.getLadvId());
        details.put("sourceId", staged.getSource().getId());
        details.put("competitionId", saved.getId());
        details.put("name", saved.getName());
        auditLogService.log(actor, AuditAction.LADV_EVENT_ADOPTED,
                "LADV_STAGED_EVENT", String.valueOf(staged.getId()), details);

        return saved;
    }

    @Transactional
    public void ignore(Long stagedId) {
        LadvStagedEvent staged = stagedRepo.findById(stagedId)
                .orElseThrow(() -> new IllegalArgumentException("Staged event not found: " + stagedId));
        if (staged.getStatus() == LadvStagedEventStatus.IMPORTED) {
            throw new IllegalStateException("Cannot ignore an already-imported event");
        }
        staged.setStatus(LadvStagedEventStatus.IGNORED);
        stagedRepo.save(staged);

        User actor = securityUtils.getCurrentUser();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ladvId", staged.getLadvId());
        details.put("name", staged.getName());
        auditLogService.log(actor, AuditAction.LADV_EVENT_IGNORED,
                "LADV_STAGED_EVENT", String.valueOf(staged.getId()), details);
    }

    @Transactional
    public void reactivate(Long stagedId) {
        LadvStagedEvent staged = stagedRepo.findById(stagedId)
                .orElseThrow(() -> new IllegalArgumentException("Staged event not found: " + stagedId));
        if (staged.getStatus() != LadvStagedEventStatus.IGNORED) {
            throw new IllegalStateException("Only IGNORED events can be reactivated");
        }
        staged.setStatus(LadvStagedEventStatus.NEW);
        stagedRepo.save(staged);
    }

    private Competition buildCompetition(LadvStagedEvent staged) {
        Competition c = new Competition();
        c.setName(staged.getName());
        c.setDate(staged.getStartDate());
        c.setStartTime(staged.getStartTime());
        c.setLatitude(staged.getLatitude());
        c.setLongitude(staged.getLongitude());
        c.setOrganizerUrl(staged.getHomepage());
        c.setSystemGenerated(true);

        // Location: prefer "PLZ Ort" if both are present.
        String location = null;
        if (staged.getOrt() != null && !staged.getOrt().isBlank()) {
            location = (staged.getPlz() != null && !staged.getPlz().isBlank())
                    ? staged.getPlz() + " " + staged.getOrt()
                    : staged.getOrt();
        }
        if (location != null) c.setLocation(truncate(location, 255));

        // Description: combine kategorie + veranstalter; cap at 1000 chars.
        StringBuilder desc = new StringBuilder();
        if (staged.getKategorie() != null && !staged.getKategorie().isBlank()) {
            desc.append(staged.getKategorie());
        }
        if (staged.getVeranstalter() != null && !staged.getVeranstalter().isBlank()) {
            if (desc.length() > 0) desc.append(" — ");
            desc.append("Veranstalter: ").append(staged.getVeranstalter());
        }
        if (desc.length() > 0) c.setDescription(truncate(desc.toString(), 1000));

        // Map laufstrecken → CompetitionFormat list with deduplicated CompetitionType.
        List<CompetitionFormat> formats = buildFormats(staged);
        if (!formats.isEmpty()) {
            c.setType(formats.get(0).getType());
        }
        c.setFormats(formats);
        return c;
    }

    private List<CompetitionFormat> buildFormats(LadvStagedEvent staged) {
        List<LadvLaufstrecke> tracks = parseTracks(staged.getRawJson());
        if (tracks == null || tracks.isEmpty()) return List.of();

        Map<CompetitionType, CompetitionFormat> byType = new LinkedHashMap<>();
        for (LadvLaufstrecke t : tracks) {
            if (t == null || t.streckeMeter == null) continue;
            CompetitionType type = mapDistanceToType(t.streckeMeter);
            String label = t.laufname != null ? t.laufname.trim() : t.streckeMeter + "m";

            CompetitionFormat existing = byType.get(type);
            if (existing == null) {
                CompetitionFormat f = new CompetitionFormat();
                f.setType(type);
                f.setStartDate(staged.getStartDate());
                f.setStartTime(staged.getStartTime());
                f.setDescription(truncate(label, 1000));
                byType.put(type, f);
            } else {
                // Same enum bucket already taken (e.g. two non-standard distances both → OTHER).
                String combined = existing.getDescription() == null
                        ? label
                        : existing.getDescription() + " · " + label;
                existing.setDescription(truncate(combined, 1000));
            }
        }
        return new ArrayList<>(byType.values());
    }

    private List<LadvLaufstrecke> parseTracks(String rawJson) {
        try {
            LadvStadionfernItem item = objectMapper.readValue(rawJson, LadvStadionfernItem.class);
            return item.laufstrecken;
        } catch (Exception e) {
            log.warn("Failed to parse raw_json for staged event tracks: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Buckets a track length (in metres) into the closest PACR
     * {@link CompetitionType}. Tolerances are tight enough to keep e.g. a
     * 4.8 km Volkslauf out of the 5K bucket — those land in {@link CompetitionType#OTHER}.
     */
    static CompetitionType mapDistanceToType(int meters) {
        if (within(meters, 5000, 200)) return CompetitionType.FIVE_K;
        if (within(meters, 10000, 300)) return CompetitionType.TEN_K;
        if (within(meters, 20000, 400)) return CompetitionType.TWENTY_K;
        if (within(meters, 21097, 200)) return CompetitionType.HALF_MARATHON;
        if (within(meters, 30000, 500)) return CompetitionType.THIRTY_K;
        if (within(meters, 40000, 500)) return CompetitionType.FORTY_K;
        if (within(meters, 42195, 200)) return CompetitionType.MARATHON;
        if (within(meters, 50000, 1000)) return CompetitionType.FIFTY_K;
        if (within(meters, 100000, 2000)) return CompetitionType.HUNDRED_K;
        return CompetitionType.OTHER;
    }

    private static boolean within(int value, int target, int tolerance) {
        return Math.abs(value - target) <= tolerance;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
