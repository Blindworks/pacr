package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_news")
public class AppNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 500)
    private String excerpt;

    @Column(name = "topic_tag", length = 64)
    private String topicTag;

    @Column(name = "hero_image_filename", length = 255)
    private String heroImageFilename;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_published")
    private boolean isPublished = false;

    /** Unique identifier from the external feed (RSS guid or link) — used for dedup. */
    @Column(name = "external_guid", length = 500, unique = true)
    private String externalGuid;

    /** URL of the original article at the source feed provider (for "read more" links). */
    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    /** URL of the hero image at the source feed provider (referenced, not downloaded). */
    @Column(name = "external_image_url", length = 1000)
    private String externalImageUrl;

    /** ISO 639-1 language code for this news item (e.g. "de", "en"). Null for legacy manual items. */
    @Column(name = "language", length = 2)
    private String language;

    /** Reference to the {@link ExternalNewsSource} this news was imported from (null for manually created news). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_source_id")
    private ExternalNewsSource externalSource;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getTopicTag() { return topicTag; }
    public void setTopicTag(String topicTag) { this.topicTag = topicTag; }
    public String getHeroImageFilename() { return heroImageFilename; }
    public void setHeroImageFilename(String heroImageFilename) { this.heroImageFilename = heroImageFilename; }
    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }
    public String getExternalGuid() { return externalGuid; }
    public void setExternalGuid(String externalGuid) { this.externalGuid = externalGuid; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public String getExternalImageUrl() { return externalImageUrl; }
    public void setExternalImageUrl(String externalImageUrl) { this.externalImageUrl = externalImageUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public ExternalNewsSource getExternalSource() { return externalSource; }
    public void setExternalSource(ExternalNewsSource externalSource) { this.externalSource = externalSource; }
}
