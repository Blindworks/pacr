package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Configuration entry for an external RSS/Atom feed that is periodically imported
 * as {@link AppNews} entries by the news importer scheduler.
 */
@Entity
@Table(name = "external_news_source")
public class ExternalNewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable source name, e.g. "Runner's World DE". Unique. */
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    /** URL of the RSS/Atom feed. */
    @Column(name = "feed_url", nullable = false, length = 1000)
    private String feedUrl;

    /** ISO 639-1 language code of the feed content (e.g. "de", "en"). */
    @Column(nullable = false, length = 2)
    private String language;

    /** If false, the scheduler skips this source. */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Timestamp of the last successful or failed fetch attempt. */
    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    /** Short status message, e.g. "success: 3 new items" or "error: connection timed out". */
    @Column(name = "last_fetch_status", length = 500)
    private String lastFetchStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFeedUrl() { return feedUrl; }
    public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
    public String getLastFetchStatus() { return lastFetchStatus; }
    public void setLastFetchStatus(String lastFetchStatus) { this.lastFetchStatus = lastFetchStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
