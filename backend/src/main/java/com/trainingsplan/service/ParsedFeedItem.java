package com.trainingsplan.service;

import java.time.LocalDateTime;

/**
 * Normalized representation of a single item from an RSS/Atom feed — independent
 * of the Rome-specific classes so downstream code stays decoupled.
 *
 * @param guid       Stable identifier for deduplication (RSS guid or link as fallback).
 * @param title      Article headline (plain text).
 * @param excerpt    Short teaser text (plain text, HTML stripped, trimmed to ~500 chars).
 * @param url        Link to the original article on the source site.
 * @param imageUrl   Optional hero image URL from the source site. May be null.
 * @param publishedAt Publication timestamp from the feed, or null if missing.
 */
public record ParsedFeedItem(
        String guid,
        String title,
        String excerpt,
        String url,
        String imageUrl,
        LocalDateTime publishedAt
) {}
