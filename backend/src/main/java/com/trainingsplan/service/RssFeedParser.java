package com.trainingsplan.service;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Fetches an RSS/Atom feed over HTTP and converts it into {@link ParsedFeedItem}s.
 * <p>
 * Uses the same {@link HttpClient} pattern as {@code ReverseGeocodingService}
 * (Java 11+ HttpClient, short timeouts, identifiable User-Agent).
 * <p>
 * RSS/Atom parsing is delegated to the Rome library which handles both formats
 * and all common date encodings transparently.
 */
@Service
public class RssFeedParser {

    private static final Logger log = LoggerFactory.getLogger(RssFeedParser.class);

    /** Maximum excerpt length in characters (matches {@code app_news.excerpt} column size). */
    private static final int MAX_EXCERPT_LENGTH = 500;

    /** Strips HTML tags — good enough for feed descriptions which are usually short. */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    /** Collapses runs of whitespace so we don't leak formatting into the excerpt. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Fetches the given feed URL and returns the parsed items.
     *
     * @throws FeedFetchException on any network, HTTP or parse error.
     */
    public List<ParsedFeedItem> fetch(String feedUrl) {
        byte[] body;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(feedUrl))
                    .header("User-Agent", "PACR/1.0 (+https://pacr.app)")
                    .header("Accept", "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FeedFetchException("HTTP " + response.statusCode() + " from " + feedUrl);
            }
            body = response.body();
        } catch (FeedFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new FeedFetchException("Failed to fetch feed: " + feedUrl, e);
        }

        SyndFeed feed;
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
            feed = new SyndFeedInput().build(reader);
        } catch (Exception e) {
            throw new FeedFetchException("Failed to parse feed: " + feedUrl, e);
        }

        List<ParsedFeedItem> items = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            try {
                ParsedFeedItem item = toItem(entry);
                if (item != null) items.add(item);
            } catch (Exception e) {
                log.warn("Skipping unparseable entry in feed {}: {}", feedUrl, e.getMessage());
            }
        }
        return items;
    }

    private ParsedFeedItem toItem(SyndEntry entry) {
        String link = entry.getLink();
        String guid = entry.getUri();
        if (guid == null || guid.isBlank()) guid = link;
        if (guid == null || guid.isBlank()) return null;

        String title = entry.getTitle();
        if (title == null || title.isBlank()) return null;
        title = plainText(title);

        String excerpt = extractExcerpt(entry);
        String imageUrl = extractImageUrl(entry);
        LocalDateTime publishedAt = extractDate(entry);

        return new ParsedFeedItem(guid.trim(), title, excerpt, link, imageUrl, publishedAt);
    }

    private String extractExcerpt(SyndEntry entry) {
        String raw = null;
        SyndContent description = entry.getDescription();
        if (description != null && description.getValue() != null) {
            raw = description.getValue();
        } else if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            SyndContent c = entry.getContents().get(0);
            if (c != null) raw = c.getValue();
        }
        if (raw == null) return null;
        return trimExcerpt(plainText(raw));
    }

    private String extractImageUrl(SyndEntry entry) {
        // Enclosure: classic RSS pattern for attached media — works for most running feeds.
        // (MediaRSS support can be added later via the optional rome-modules dependency.)
        List<SyndEnclosure> enclosures = entry.getEnclosures();
        if (enclosures != null) {
            for (SyndEnclosure e : enclosures) {
                String type = e.getType();
                if (type != null && type.toLowerCase().startsWith("image/") && e.getUrl() != null) {
                    return e.getUrl();
                }
            }
        }
        return null;
    }

    private LocalDateTime extractDate(SyndEntry entry) {
        Date d = entry.getPublishedDate();
        if (d == null) d = entry.getUpdatedDate();
        if (d == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneId.systemDefault());
    }

    private String plainText(String input) {
        if (input == null) return null;
        String stripped = HTML_TAG.matcher(input).replaceAll(" ");
        // Decode the few HTML entities that commonly appear in feed descriptions.
        stripped = stripped
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        return WHITESPACE.matcher(stripped).replaceAll(" ").trim();
    }

    private String trimExcerpt(String s) {
        if (s == null) return null;
        if (s.length() <= MAX_EXCERPT_LENGTH) return s;
        int cut = s.lastIndexOf(' ', MAX_EXCERPT_LENGTH - 3);
        if (cut < MAX_EXCERPT_LENGTH - 100) cut = MAX_EXCERPT_LENGTH - 3;
        return s.substring(0, cut).trim() + "...";
    }
}
