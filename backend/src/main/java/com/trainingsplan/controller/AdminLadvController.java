package com.trainingsplan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ladv.LadvCreateSourceRequest;
import com.trainingsplan.dto.ladv.LadvImportRunSummary;
import com.trainingsplan.dto.ladv.LadvImportSourceDto;
import com.trainingsplan.dto.ladv.LadvLvOption;
import com.trainingsplan.dto.ladv.LadvStadionfernItem;
import com.trainingsplan.dto.ladv.LadvStagedEventDto;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.LadvImportSource;
import com.trainingsplan.entity.LadvStagedEvent;
import com.trainingsplan.entity.LadvStagedEventStatus;
import com.trainingsplan.repository.LadvImportSourceRepository;
import com.trainingsplan.repository.LadvStagedEventRepository;
import com.trainingsplan.service.ladv.LadvApiException;
import com.trainingsplan.service.ladv.LadvCompetitionAdoptionService;
import com.trainingsplan.service.ladv.LadvImporterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin endpoints for the LADV PUBLIC API import: configure sources, fetch
 * events into staging, adopt them as competitions, or skip them.
 */
@RestController
@RequestMapping("/api/admin/ladv")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLadvController {

    /** Subset of LADV Landesverbandskennzeichen we surface in the admin form. */
    private static final List<LadvLvOption> SUPPORTED_LVS = List.of(
            new LadvLvOption("BA", "Baden"),
            new LadvLvOption("BB", "Brandenburg"),
            new LadvLvOption("BE", "Berlin"),
            new LadvLvOption("BR", "Bremen"),
            new LadvLvOption("BY", "Bayern"),
            new LadvLvOption("HE", "Hessen"),
            new LadvLvOption("HH", "Hamburg"),
            new LadvLvOption("MV", "Mecklenburg-Vorpommern"),
            new LadvLvOption("NI", "Niedersachsen"),
            new LadvLvOption("NO", "Nordrhein"),
            new LadvLvOption("PF", "Pfalz"),
            new LadvLvOption("RH", "Rheinhessen"),
            new LadvLvOption("RL", "Rheinland"),
            new LadvLvOption("SH", "Schleswig-Holstein"),
            new LadvLvOption("SL", "Saarland"),
            new LadvLvOption("SN", "Sachsen"),
            new LadvLvOption("ST", "Sachsen-Anhalt"),
            new LadvLvOption("TH", "Thüringen"),
            new LadvLvOption("WE", "Westfalen"),
            new LadvLvOption("WÜ", "Württemberg")
    );
    private static final Set<String> VALID_LV_CODES = SUPPORTED_LVS.stream()
            .map(LadvLvOption::code)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final LadvImportSourceRepository sourceRepo;
    private final LadvStagedEventRepository stagedRepo;
    private final LadvImporterService importer;
    private final LadvCompetitionAdoptionService adopter;
    private final ObjectMapper objectMapper;

    public AdminLadvController(LadvImportSourceRepository sourceRepo,
                               LadvStagedEventRepository stagedRepo,
                               LadvImporterService importer,
                               LadvCompetitionAdoptionService adopter,
                               ObjectMapper objectMapper) {
        this.sourceRepo = sourceRepo;
        this.stagedRepo = stagedRepo;
        this.importer = importer;
        this.adopter = adopter;
        this.objectMapper = objectMapper;
    }

    // ---------- Sources ----------

    @GetMapping("/sources")
    public List<LadvImportSourceDto> listSources() {
        return sourceRepo.findAll().stream()
                .sorted(Comparator.comparing(LadvImportSource::getName, String.CASE_INSENSITIVE_ORDER))
                .map(LadvImportSourceDto::from)
                .toList();
    }

    @GetMapping("/sources/{id}")
    public ResponseEntity<?> getSource(@PathVariable Long id) {
        return sourceRepo.findById(id)
                .map(s -> ResponseEntity.ok((Object) LadvImportSourceDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sources")
    public ResponseEntity<?> createSource(@RequestBody LadvCreateSourceRequest req) {
        String error = validate(req);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("message", error));
        if (sourceRepo.findByName(req.name().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Source name already exists"));
        }
        LadvImportSource s = new LadvImportSource();
        s.setName(req.name().trim());
        s.setLv(req.lv().trim());
        s.setBestenlistenfaehigOnly(Boolean.TRUE.equals(req.bestenlistenfaehigOnly()));
        s.setEnabled(req.enabled() == null || req.enabled());
        s.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(LadvImportSourceDto.from(sourceRepo.save(s)));
    }

    @PutMapping("/sources/{id}")
    public ResponseEntity<?> updateSource(@PathVariable Long id, @RequestBody LadvCreateSourceRequest req) {
        String error = validate(req);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("message", error));
        LadvImportSource s = sourceRepo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        String newName = req.name().trim();
        if (!s.getName().equalsIgnoreCase(newName)
                && sourceRepo.findByName(newName).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Source name already exists"));
        }
        s.setName(newName);
        s.setLv(req.lv().trim());
        s.setBestenlistenfaehigOnly(Boolean.TRUE.equals(req.bestenlistenfaehigOnly()));
        s.setEnabled(req.enabled() == null || req.enabled());
        return ResponseEntity.ok(LadvImportSourceDto.from(sourceRepo.save(s)));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<?> deleteSource(@PathVariable Long id) {
        if (!sourceRepo.existsById(id)) return ResponseEntity.notFound().build();
        sourceRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sources/{id}/fetch")
    public ResponseEntity<?> fetchNow(@PathVariable Long id) {
        try {
            LadvImportRunSummary summary = importer.importFromSource(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (LadvApiException e) {
            HttpStatus status = e.getStatusCode() == 401 ? HttpStatus.UNAUTHORIZED
                    : e.getStatusCode() == 403 ? HttpStatus.FORBIDDEN
                    : HttpStatus.BAD_GATEWAY;
            return ResponseEntity.status(status).body(Map.of("message", e.getMessage()));
        }
    }

    // ---------- Staged events ----------

    @GetMapping("/events")
    public Map<String, Object> listEvents(@RequestParam(required = false) Long sourceId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        LadvStagedEventStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = LadvStagedEventStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        String query = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<LadvStagedEvent> p = stagedRepo.search(sourceId, statusEnum, query,
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size))));
        List<LadvStagedEventDto> content = p.getContent().stream()
                .map(this::toDto)
                .toList();
        return Map.of(
                "content", content,
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages(),
                "page", p.getNumber(),
                "size", p.getSize()
        );
    }

    @PostMapping("/events/{id}/adopt")
    public ResponseEntity<?> adopt(@PathVariable Long id) {
        try {
            Competition saved = adopter.adopt(id);
            return ResponseEntity.ok(Map.of("competitionId", saved.getId(), "name", saved.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/events/{id}/ignore")
    public ResponseEntity<?> ignore(@PathVariable Long id) {
        try {
            adopter.ignore(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/events/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        try {
            adopter.reactivate(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // ---------- Static lookup ----------

    @GetMapping("/lvs")
    public List<LadvLvOption> listLvs() {
        return SUPPORTED_LVS;
    }

    // ---------- Helpers ----------

    private String validate(LadvCreateSourceRequest req) {
        if (req == null) return "Body missing";
        if (req.name() == null || req.name().trim().isEmpty()) return "Name is required";
        if (req.lv() == null || req.lv().trim().isEmpty()) return "Landesverband is required";
        if (!VALID_LV_CODES.contains(req.lv().trim())) {
            return "Unknown Landesverband code: " + req.lv();
        }
        return null;
    }

    private LadvStagedEventDto toDto(LadvStagedEvent e) {
        List<LadvStagedEventDto.DistanceDto> distances = new ArrayList<>();
        try {
            LadvStadionfernItem item = objectMapper.readValue(e.getRawJson(), LadvStadionfernItem.class);
            if (item.laufstrecken != null) {
                for (var t : item.laufstrecken) {
                    if (t == null) continue;
                    distances.add(new LadvStagedEventDto.DistanceDto(
                            t.laufname,
                            t.streckeMeter
                    ));
                }
            }
        } catch (Exception ignored) {
            // Distances are best-effort; an unparseable raw_json should not break the listing.
        }
        return new LadvStagedEventDto(
                e.getId(),
                e.getSource().getId(),
                e.getSource().getName(),
                e.getLadvId(),
                e.getVeranstaltungsnummer(),
                e.getName(),
                e.getStartDate(),
                e.getEndDate(),
                e.getStartTime(),
                e.getKategorie(),
                e.getVeranstalter(),
                e.getHomepage(),
                e.getOrt(),
                e.getPlz(),
                e.getLatitude(),
                e.getLongitude(),
                e.isAbgesagt(),
                distances,
                e.getStatus(),
                e.getImportedCompetition() != null ? e.getImportedCompetition().getId() : null,
                e.getImportedAt(),
                e.getFetchedAt()
        );
    }
}
