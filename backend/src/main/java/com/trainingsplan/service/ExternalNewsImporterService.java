package com.trainingsplan.service;

import com.trainingsplan.dto.ImportRunSummary;
import com.trainingsplan.entity.AppNews;
import com.trainingsplan.entity.ExternalNewsSource;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.repository.ExternalNewsSourceRepository;
import com.trainingsplan.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates the import of external RSS/Atom feeds into the PACR news system.
 * <p>
 * Per source: fetches the feed, iterates items, deduplicates via external GUID,
 * and persists new items via {@link AppNewsService#createFromExternal}. Errors
 * in one source do not affect other sources.
 */
@Service
public class ExternalNewsImporterService {

    private static final Logger log = LoggerFactory.getLogger(ExternalNewsImporterService.class);

    private final ExternalNewsSourceRepository sourceRepo;
    private final UserRepository userRepository;
    private final RssFeedParser rssFeedParser;
    private final AppNewsService newsService;

    public ExternalNewsImporterService(ExternalNewsSourceRepository sourceRepo,
                                       UserRepository userRepository,
                                       RssFeedParser rssFeedParser,
                                       AppNewsService newsService) {
        this.sourceRepo = sourceRepo;
        this.userRepository = userRepository;
        this.rssFeedParser = rssFeedParser;
        this.newsService = newsService;
    }

    /** Iterates every enabled source and imports new items. */
    public ImportRunSummary importAllEnabled() {
        List<ExternalNewsSource> enabled = sourceRepo.findAllByEnabledTrue();
        List<ImportRunSummary.SourceResult> results = new ArrayList<>(enabled.size());
        int totalNew = 0;
        User systemUser = resolveSystemUser();
        for (ExternalNewsSource source : enabled) {
            ImportRunSummary.SourceResult r = importOne(source, systemUser);
            results.add(r);
            totalNew += r.newItems();
        }
        return new ImportRunSummary(enabled.size(), totalNew, results);
    }

    /** Imports a single source on demand (used by the admin "fetch now" button). */
    public ImportRunSummary importFromSource(Long sourceId) {
        ExternalNewsSource source = sourceRepo.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));
        User systemUser = resolveSystemUser();
        ImportRunSummary.SourceResult r = importOne(source, systemUser);
        return new ImportRunSummary(1, r.newItems(), List.of(r));
    }

    @Transactional
    protected ImportRunSummary.SourceResult importOne(ExternalNewsSource source, User systemUser) {
        int newItems = 0;
        int skipped = 0;
        try {
            List<ParsedFeedItem> items = rssFeedParser.fetch(source.getFeedUrl());
            for (ParsedFeedItem item : items) {
                AppNews created = newsService.createFromExternal(source, item, systemUser);
                if (created == null) {
                    skipped++;
                } else {
                    newItems++;
                }
            }
            String status = "success: " + newItems + " new, " + skipped + " skipped";
            updateSourceStatus(source, status);
            return new ImportRunSummary.SourceResult(
                    source.getId(), source.getName(), newItems, skipped, true, status);
        } catch (FeedFetchException e) {
            String msg = "error: " + safeMessage(e);
            log.warn("Feed import failed for {}: {}", source.getName(), msg);
            updateSourceStatus(source, msg);
            return new ImportRunSummary.SourceResult(
                    source.getId(), source.getName(), 0, 0, false, msg);
        } catch (Exception e) {
            String msg = "error: " + safeMessage(e);
            log.error("Unexpected error importing {}: {}", source.getName(), msg, e);
            updateSourceStatus(source, msg);
            return new ImportRunSummary.SourceResult(
                    source.getId(), source.getName(), 0, 0, false, msg);
        }
    }

    private void updateSourceStatus(ExternalNewsSource source, String status) {
        source.setLastFetchedAt(LocalDateTime.now());
        source.setLastFetchStatus(truncate(status, 500));
        sourceRepo.save(source);
    }

    /**
     * System user for {@code AppNews.createdBy} (NOT NULL in schema). Picks the
     * oldest admin — stable across deployments and avoids yet another user row.
     */
    private User resolveSystemUser() {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        if (admins.isEmpty()) {
            throw new IllegalStateException("No ADMIN user found to use as news import author");
        }
        return admins.stream()
                .min(Comparator.comparing(User::getId))
                .orElse(admins.get(0));
    }

    private String safeMessage(Throwable t) {
        String m = t.getMessage();
        if (m == null || m.isBlank()) return t.getClass().getSimpleName();
        return m;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
