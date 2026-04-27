package com.trainingsplan.service.ladv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ladv.LadvStadionfernItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * HTTP client for the LADV PUBLIC API v2.014 (https://ladv.de/entwickler).
 * Currently exposes the {@code /stadionfern} endpoint, which lists running
 * events outside stadia (Strassenlauf / Volkslauf / Berglauf / Crosslauf).
 *
 * Auth: API key embedded in the URL path. Only keys with Landesverbandsdaten
 * access can call /stadionfern — the server returns HTTP 401 otherwise.
 */
@Service
public class LadvApiClient {

    private static final Logger log = LoggerFactory.getLogger(LadvApiClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String userAgent;
    private final Duration requestTimeout;

    public LadvApiClient(ObjectMapper objectMapper,
                         @Value("${ladv.api.base-url:https://ladv.de/api}") String baseUrl,
                         @Value("${ladv.api.key:}") String apiKey,
                         @Value("${ladv.api.user-agent:PACR/1.0 (+https://pacr.app)}") String userAgent,
                         @Value("${ladv.api.timeout-seconds:10}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.userAgent = userAgent;
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Calls {@code /stadionfern} for the given Landesverband.
     *
     * @param lv LADV Landesverbandskennzeichen (e.g. {@code BY}, {@code WUE})
     * @param year four-digit year (LADV {@code datayear} param)
     * @param bestenlistenfaehigOnly if true, append the {@code bestenlistenfaehig} flag
     * @param limit 1..500
     */
    public List<LadvStadionfernItem> fetchStadionfern(String lv,
                                                      int year,
                                                      boolean bestenlistenfaehigOnly,
                                                      int limit) {
        if (apiKey.isEmpty()) {
            throw new LadvApiException("LADV API key not configured (set LADV_API_KEY)", 0);
        }
        StringBuilder url = new StringBuilder(baseUrl)
                .append('/').append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                .append("/stadionfern")
                .append("?lv=").append(URLEncoder.encode(lv, StandardCharsets.UTF_8))
                .append("&datayear=").append(year)
                .append("&limit=").append(clamp(limit, 1, 500));
        if (bestenlistenfaehigOnly) url.append("&bestenlistenfaehig=true");

        // Build a redacted version for logs that hides the API key.
        String redacted = url.toString().replace(apiKey, "***");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .timeout(requestTimeout)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status == 401) {
                throw new LadvApiException(
                        "LADV refused: API key has no Landesverbandsdaten access (HTTP 401)", 401);
            }
            if (status == 403) {
                throw new LadvApiException("LADV refused: forbidden (HTTP 403)", 403);
            }
            if (status < 200 || status >= 300) {
                throw new LadvApiException("LADV returned HTTP " + status + " for " + redacted, status);
            }

            return objectMapper.readValue(response.body(),
                    new TypeReference<List<LadvStadionfernItem>>() {});
        } catch (LadvApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("LADV fetch failed for {}: {}", redacted, e.getMessage());
            throw new LadvApiException("LADV fetch failed: " + e.getMessage(), e);
        }
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
