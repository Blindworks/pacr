package com.trainingsplan.service.ladv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ladv.LadvImportRunSummary;
import com.trainingsplan.dto.ladv.LadvStadionfernItem;
import com.trainingsplan.dto.ladv.LadvStartstelle;
import com.trainingsplan.entity.LadvImportSource;
import com.trainingsplan.entity.LadvStagedEvent;
import com.trainingsplan.entity.LadvStagedEventStatus;
import com.trainingsplan.repository.LadvImportSourceRepository;
import com.trainingsplan.repository.LadvStagedEventRepository;
import com.trainingsplan.service.ForwardGeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Fetches LADV stadionfern events for a configured source and persists them
 * into the {@code ladv_staged_event} staging table. Admins then decide
 * per-event whether to adopt them as PACR competitions
 * (see {@link LadvCompetitionAdoptionService}).
 *
 * Patterns mirror {@code ExternalNewsImporterService}: dedup by (source, externalId),
 * record success/error in {@code last_fetch_status}, never let one event break the run.
 */
@Service
public class LadvImporterService {

    private static final Logger log = LoggerFactory.getLogger(LadvImporterService.class);
    /** LADV epoch values are emitted at 0:00 of the German calendar day. */
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final int DEFAULT_LIMIT = 500;

    private final LadvImportSourceRepository sourceRepo;
    private final LadvStagedEventRepository stagedRepo;
    private final LadvApiClient apiClient;
    private final ForwardGeocodingService geocoder;
    private final ObjectMapper objectMapper;

    public LadvImporterService(LadvImportSourceRepository sourceRepo,
                               LadvStagedEventRepository stagedRepo,
                               LadvApiClient apiClient,
                               ForwardGeocodingService geocoder,
                               ObjectMapper objectMapper) {
        this.sourceRepo = sourceRepo;
        this.stagedRepo = stagedRepo;
        this.apiClient = apiClient;
        this.geocoder = geocoder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LadvImportRunSummary importFromSource(Long sourceId) {
        LadvImportSource source = sourceRepo.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("LADV source not found: " + sourceId));
        return importOne(source);
    }

    @Transactional
    public LadvImportRunSummary importOne(LadvImportSource source) {
        int year = Year.now(BERLIN).getValue();
        int fetched = 0;
        int newItems = 0;
        int skipped = 0;
        try {
            List<LadvStadionfernItem> items = apiClient.fetchStadionfern(
                    source.getLv(), year, source.isBestenlistenfaehigOnly(), DEFAULT_LIMIT);
            fetched = items.size();
            for (LadvStadionfernItem item : items) {
                if (item == null || item.id == null || item.name == null) {
                    skipped++;
                    continue;
                }
                if (stagedRepo.findBySourceIdAndLadvId(source.getId(), item.id).isPresent()) {
                    skipped++;
                    continue;
                }
                try {
                    LadvStagedEvent staged = toStaged(source, item);
                    stagedRepo.save(staged);
                    newItems++;
                } catch (Exception e) {
                    log.warn("Skipping unparseable LADV item id={} from source {}: {}",
                            item.id, source.getName(), e.getMessage());
                    skipped++;
                }
            }
            String status = "success: " + newItems + " new, " + skipped + " skipped of " + fetched;
            updateSourceStatus(source, status);
            return new LadvImportRunSummary(source.getId(), source.getName(),
                    fetched, newItems, skipped, true, status);
        } catch (LadvApiException e) {
            String msg = "error: " + safeMessage(e);
            log.warn("LADV import failed for {}: {}", source.getName(), msg);
            updateSourceStatus(source, msg);
            return new LadvImportRunSummary(source.getId(), source.getName(),
                    fetched, newItems, skipped, false, msg);
        } catch (Exception e) {
            String msg = "error: " + safeMessage(e);
            log.error("Unexpected error importing LADV source {}: {}", source.getName(), msg, e);
            updateSourceStatus(source, msg);
            return new LadvImportRunSummary(source.getId(), source.getName(),
                    fetched, newItems, skipped, false, msg);
        }
    }

    private LadvStagedEvent toStaged(LadvImportSource source, LadvStadionfernItem item)
            throws JsonProcessingException {
        LadvStagedEvent staged = new LadvStagedEvent();
        staged.setSource(source);
        staged.setLadvId(item.id);
        staged.setVeranstaltungsnummer(truncate(item.veranstaltungsnummer, 40));
        staged.setName(truncate(item.name, 255));
        staged.setStartDate(toLocalDate(item.datum));
        staged.setEndDate(toLocalDate(item.endeDatum));
        staged.setStartTime(parseTime(item.beginn));
        staged.setKategorie(truncate(item.kategorie, 255));
        staged.setVeranstalter(truncate(item.veranstalter, 255));
        staged.setHomepage(truncate(item.homepage, 500));

        String ort = null;
        String plz = null;
        LadvStartstelle ss = item.startstelle;
        if (ss != null) {
            ort = ss.ort;
            plz = ss.plz;
        }
        if ((ort == null || ort.isBlank()) && item.organisator != null) {
            ort = item.organisator.ort;
        }
        if ((plz == null || plz.isBlank()) && item.organisator != null) {
            plz = item.organisator.plz;
        }
        staged.setOrt(truncate(ort, 255));
        staged.setPlz(truncate(plz, 15));

        // Best-effort geocoding — never block the import on a Nominatim outage.
        if (ort != null && !ort.isBlank()) {
            String query = (plz != null && !plz.isBlank() ? plz + " " : "") + ort;
            ForwardGeocodingService.LatLng coords = geocoder.geocode(query);
            if (coords != null) {
                staged.setLatitude(coords.lat());
                staged.setLongitude(coords.lng());
            }
        }

        staged.setAbgesagt(Boolean.TRUE.equals(item.abgesagt));
        staged.setRawJson(objectMapper.writeValueAsString(item));
        staged.setStatus(LadvStagedEventStatus.NEW);
        staged.setFetchedAt(LocalDateTime.now());

        if (staged.getStartDate() == null) {
            // LADV data without a start date is useless for us — skip via exception.
            throw new IllegalArgumentException("Event has no start date");
        }
        return staged;
    }

    private void updateSourceStatus(LadvImportSource source, String status) {
        source.setLastFetchedAt(LocalDateTime.now());
        source.setLastFetchStatus(truncate(status, 500));
        sourceRepo.save(source);
    }

    private static LocalDate toLocalDate(Long epochMillis) {
        if (epochMillis == null) return null;
        return Instant.ofEpochMilli(epochMillis).atZone(BERLIN).toLocalDate();
    }

    private static LocalTime parseTime(String beginn) {
        if (beginn == null || beginn.isBlank()) return null;
        String s = beginn.trim();
        // LADV typically gives "10:00" but values like "10.00" or "10" appear in the wild.
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException ignored) {
            // Try common alternates.
            String normalized = s.replace('.', ':');
            if (normalized.matches("\\d{1,2}:\\d{2}")) {
                try { return LocalTime.parse(normalized.length() == 4 ? "0" + normalized : normalized); }
                catch (Exception ignored2) {}
            }
            if (s.matches("\\d{1,2}")) {
                try { return LocalTime.of(Integer.parseInt(s), 0); } catch (Exception ignored3) {}
            }
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        if (m == null || m.isBlank()) return t.getClass().getSimpleName();
        return m;
    }
}
