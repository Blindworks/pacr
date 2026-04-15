package com.trainingsplan.controller;

import com.trainingsplan.dto.CreateExternalNewsSourceRequest;
import com.trainingsplan.dto.ExternalNewsSourceDto;
import com.trainingsplan.dto.ImportRunSummary;
import com.trainingsplan.entity.ExternalNewsSource;
import com.trainingsplan.repository.AppNewsRepository;
import com.trainingsplan.repository.ExternalNewsSourceRepository;
import com.trainingsplan.service.ExternalNewsImporterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin CRUD + manual-fetch endpoints for configuring external RSS news sources.
 */
@RestController
@RequestMapping("/api/admin/news-sources")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExternalNewsSourceController {

    /** Language codes accepted for a news source — keep in sync with frontend language picker. */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("de", "en");

    private final ExternalNewsSourceRepository sourceRepo;
    private final AppNewsRepository newsRepo;
    private final ExternalNewsImporterService importer;

    public AdminExternalNewsSourceController(ExternalNewsSourceRepository sourceRepo,
                                             AppNewsRepository newsRepo,
                                             ExternalNewsImporterService importer) {
        this.sourceRepo = sourceRepo;
        this.newsRepo = newsRepo;
        this.importer = importer;
    }

    @GetMapping
    public List<ExternalNewsSourceDto> list() {
        return sourceRepo.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateExternalNewsSourceRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", validationError));
        }
        if (sourceRepo.findByName(request.name().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Source name already exists"));
        }
        ExternalNewsSource source = new ExternalNewsSource();
        source.setName(request.name().trim());
        source.setFeedUrl(request.feedUrl().trim());
        source.setLanguage(request.language().toLowerCase());
        source.setEnabled(request.enabled() == null || request.enabled());
        source.setCreatedAt(LocalDateTime.now());
        ExternalNewsSource saved = sourceRepo.save(source);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CreateExternalNewsSourceRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", validationError));
        }
        ExternalNewsSource source = sourceRepo.findById(id).orElse(null);
        if (source == null) return ResponseEntity.notFound().build();
        String newName = request.name().trim();
        if (!source.getName().equalsIgnoreCase(newName)
                && sourceRepo.findByName(newName).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Source name already exists"));
        }
        source.setName(newName);
        source.setFeedUrl(request.feedUrl().trim());
        source.setLanguage(request.language().toLowerCase());
        source.setEnabled(request.enabled() == null || request.enabled());
        ExternalNewsSource saved = sourceRepo.save(source);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        ExternalNewsSource source = sourceRepo.findById(id).orElse(null);
        if (source == null) return ResponseEntity.notFound().build();
        long referenced = newsRepo.countByExternalSource_Id(id);
        if (referenced > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Cannot delete source: " + referenced + " news item(s) reference it",
                            "referencedNewsCount", referenced
                    ));
        }
        sourceRepo.delete(source);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fetch")
    public ResponseEntity<?> fetchNow(@PathVariable Long id) {
        try {
            ImportRunSummary summary = importer.importFromSource(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String validate(CreateExternalNewsSourceRequest request) {
        if (request == null) return "Body missing";
        if (request.name() == null || request.name().trim().isEmpty()) return "Name is required";
        if (request.feedUrl() == null || request.feedUrl().trim().isEmpty()) return "Feed URL is required";
        String url = request.feedUrl().trim().toLowerCase();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "Feed URL must start with http:// or https://";
        }
        if (request.language() == null
                || !SUPPORTED_LANGUAGES.contains(request.language().toLowerCase())) {
            return "Language must be one of: " + SUPPORTED_LANGUAGES;
        }
        return null;
    }

    private ExternalNewsSourceDto toDto(ExternalNewsSource s) {
        return new ExternalNewsSourceDto(
                s.getId(),
                s.getName(),
                s.getFeedUrl(),
                s.getLanguage(),
                s.isEnabled(),
                s.getLastFetchedAt(),
                s.getLastFetchStatus(),
                s.getCreatedAt()
        );
    }
}
