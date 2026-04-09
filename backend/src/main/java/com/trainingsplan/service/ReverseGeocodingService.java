package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal reverse-geocoding client for Nominatim (OpenStreetMap).
 * Used by the admin GPX upload flow to tag community routes with their nearest city.
 *
 * Nominatim usage policy: 1 req/s, meaningful User-Agent.
 */
@Service
public class ReverseGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(ReverseGeocodingService.class);
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%s&lon=%s&zoom=10&accept-language=en";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    public ReverseGeocodingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the nearest populated place (city / town / village / municipality)
     * for the given coordinates, or null if lookup fails.
     */
    public String findNearestCity(double lat, double lon) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(java.util.Locale.ROOT, NOMINATIM_URL, lat, lon)))
                    .header("User-Agent", "PACR/1.0 (pacr.app)")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Nominatim returned {} for {},{}", response.statusCode(), lat, lon);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode address = root.path("address");
            if (address.isMissingNode() || address.isNull()) {
                return null;
            }

            // Try most-specific populated place first, then fall back.
            String[] keys = {"city", "town", "village", "municipality", "hamlet", "suburb", "county", "state"};
            for (String key : keys) {
                JsonNode node = address.get(key);
                if (node != null && !node.isNull() && !node.asText().isBlank()) {
                    return node.asText();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Reverse geocoding failed for {},{}: {}", lat, lon, e.getMessage());
            return null;
        }
    }
}
