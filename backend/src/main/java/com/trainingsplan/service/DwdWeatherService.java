package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.BioWeatherDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class DwdWeatherService {

    private static final Logger log = LoggerFactory.getLogger(DwdWeatherService.class);

    private static final String POLLEN_URL = "https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json";
    private static final String BIO_URL   = "https://opendata.dwd.de/climate_environment/health/alerts/biowetter.json";
    private static final String WEATHER_URL_TEMPLATE =
            "https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}" +
            "&current=temperature_2m,relative_humidity_2m&timezone=auto";
    private static final String AIR_URL_TEMPLATE =
            "https://air-quality-api.open-meteo.com/v1/air-quality?latitude={lat}&longitude={lon}" +
            "&current=pm2_5,ozone&timezone=auto";
    private static final Duration CACHE_TTL = Duration.ofHours(4);

    /** Approximate centre coordinates for each DWD pollen region */
    private static final java.util.Map<Integer, double[]> REGION_COORDS = java.util.Map.ofEntries(
        java.util.Map.entry(10,  new double[]{53.55,  9.99}),
        java.util.Map.entry(20,  new double[]{53.62, 12.43}),
        java.util.Map.entry(30,  new double[]{52.37,  9.74}),
        java.util.Map.entry(40,  new double[]{51.51,  7.46}),
        java.util.Map.entry(50,  new double[]{52.52, 13.40}),
        java.util.Map.entry(60,  new double[]{51.97, 11.58}),
        java.util.Map.entry(70,  new double[]{51.02, 11.00}),
        java.util.Map.entry(80,  new double[]{51.05, 13.74}),
        java.util.Map.entry(90,  new double[]{50.11,  8.68}),  // Hessen
        java.util.Map.entry(100, new double[]{49.99,  7.17}),  // Rheinland-Pfalz und Saarland
        java.util.Map.entry(110, new double[]{48.78,  9.18}),  // Baden-Württemberg
        java.util.Map.entry(120, new double[]{48.13, 11.57})   // Bayern
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String pollenCacheJson;
    private volatile Instant pollenCachedAt;

    public DwdWeatherService() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public BioWeatherDto getEnvironmentData(int regionId) {
        BioWeatherDto dto = new BioWeatherDto();
        dto.setRegionId(regionId);
        dto.setDataSource("DWD OpenData");
        dto.setValidDate(LocalDate.now());

        // Pollen (cached)
        try {
            String pollenJson = fetchPollenCached();
            if (pollenJson != null) {
                JsonNode root = objectMapper.readTree(pollenJson);
                JsonNode content = root.path("content");
                if (content.isArray()) {
                    for (JsonNode region : content) {
                        if (region.path("region_id").asInt() == regionId) {
                            dto.setRegionName(region.path("region_name").asText(null));
                            JsonNode pollen = region.path("Pollen");
                            dto.setPollenBirch(parsePollenValue(pollen, "Birke"));
                            dto.setPollenGrasses(parsePollenValue(pollen, "Gräser"));
                            dto.setPollenMugwort(parsePollenValue(pollen, "Beifuß"));
                            dto.setPollenRagweed(parsePollenValue(pollen, "Ambrosia"));
                            dto.setPollenHazel(parsePollenValue(pollen, "Hasel"));
                            dto.setPollenAlder(parsePollenValue(pollen, "Erle"));
                            dto.setPollenAsh(parsePollenValue(pollen, "Esche"));
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse pollen data: {}", e.getMessage());
        }

        // Temperature + Humidity (Open-Meteo)
        double[] coords = REGION_COORDS.getOrDefault(regionId, new double[]{51.0, 10.0});
        try {
            String weatherUrl = WEATHER_URL_TEMPLATE
                    .replace("{lat}", String.valueOf(coords[0]))
                    .replace("{lon}", String.valueOf(coords[1]));
            String weatherJson = restClient.get().uri(weatherUrl).retrieve().body(String.class);
            if (weatherJson != null) {
                JsonNode root = objectMapper.readTree(weatherJson);
                JsonNode current = root.path("current");
                if (!current.isMissingNode()) {
                    if (current.has("temperature_2m"))
                        dto.setTemperature(current.path("temperature_2m").asDouble());
                    if (current.has("relative_humidity_2m"))
                        dto.setHumidity(current.path("relative_humidity_2m").asInt());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch weather data: {}", e.getMessage());
        }

        // PM2.5 + Ozone (Open-Meteo Air Quality)
        try {
            String airUrl = AIR_URL_TEMPLATE
                    .replace("{lat}", String.valueOf(coords[0]))
                    .replace("{lon}", String.valueOf(coords[1]));
            String airJson = restClient.get().uri(airUrl).retrieve().body(String.class);
            if (airJson != null) {
                JsonNode root = objectMapper.readTree(airJson);
                JsonNode current = root.path("current");
                if (!current.isMissingNode()) {
                    if (current.has("pm2_5"))
                        dto.setPm25(current.path("pm2_5").asDouble());
                    if (current.has("ozone"))
                        dto.setOzone(current.path("ozone").asDouble());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch air quality data: {}", e.getMessage());
        }

        // Biowetter
        try {
            String bioJson = restClient.get().uri(BIO_URL).retrieve().body(String.class);
            if (bioJson != null) {
                JsonNode root = objectMapper.readTree(bioJson);
                JsonNode content = root.path("content");
                if (content.isArray() && content.size() > 0) {
                    JsonNode first = content.get(0);
                    dto.setBiowetterRisk(first.path("risk").asText(
                            first.path("Belastungsstufe").asText(null)
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch biowetter data: {}", e.getMessage());
        }

        dto.setAsthmaRiskIndex(computeAsthmaRiskIndex(dto));
        return dto;
    }

    /**
     * Asthma-Risk-Index 0–100 (higher = more risk).
     * Weighted combination of pollen (30%), PM2.5 (25%), ozone (20%),
     * humidity (15%), temperature (10%).
     */
    private int computeAsthmaRiskIndex(BioWeatherDto dto) {
        // Pollen: max of all types (0-3 scale → 0-100)
        int maxPollen = 0;
        if (dto.getPollenGrasses() != null) maxPollen = Math.max(maxPollen, dto.getPollenGrasses());
        if (dto.getPollenBirch()   != null) maxPollen = Math.max(maxPollen, dto.getPollenBirch());
        if (dto.getPollenMugwort() != null) maxPollen = Math.max(maxPollen, dto.getPollenMugwort());
        if (dto.getPollenHazel()   != null) maxPollen = Math.max(maxPollen, dto.getPollenHazel());
        if (dto.getPollenAlder()   != null) maxPollen = Math.max(maxPollen, dto.getPollenAlder());
        if (dto.getPollenAsh()     != null) maxPollen = Math.max(maxPollen, dto.getPollenAsh());
        if (dto.getPollenRagweed() != null) maxPollen = Math.max(maxPollen, dto.getPollenRagweed());
        double pollenScore = Math.min(100.0, (maxPollen / 3.0) * 100.0);

        // PM2.5: WHO 24h limit = 15 µg/m³; scale to 100 at 40 µg/m³
        double pm25Score = dto.getPm25() != null
                ? Math.min(100.0, (dto.getPm25() / 40.0) * 100.0) : 0.0;

        // Ozone: EU info threshold = 180 µg/m³
        double ozoneScore = dto.getOzone() != null
                ? Math.min(100.0, (dto.getOzone() / 180.0) * 100.0) : 0.0;

        // Humidity: optimal 40-60%; penalty outside that range
        double humidityScore = 0.0;
        if (dto.getHumidity() != null) {
            int h = dto.getHumidity();
            humidityScore = h < 40 ? Math.min(100.0, (40 - h) * 2.5)
                          : h > 60 ? Math.min(100.0, (h - 60) * 2.5) : 0.0;
        }

        // Temperature: optimal 15-22°C; penalty outside range
        double tempScore = 0.0;
        if (dto.getTemperature() != null) {
            double t = dto.getTemperature();
            tempScore = t < 5  ? Math.min(100.0, (5 - t) * 5.0)
                      : t > 28 ? Math.min(100.0, (t - 28) * 5.0) : 0.0;
        }

        double total = 0.30 * pollenScore
                     + 0.25 * pm25Score
                     + 0.20 * ozoneScore
                     + 0.15 * humidityScore
                     + 0.10 * tempScore;

        return (int) Math.round(Math.min(100.0, total));
    }

    private String fetchPollenCached() {
        Instant now = Instant.now();
        if (pollenCacheJson != null && pollenCachedAt != null
                && Duration.between(pollenCachedAt, now).compareTo(CACHE_TTL) < 0) {
            return pollenCacheJson;
        }
        try {
            String json = restClient.get().uri(POLLEN_URL).retrieve().body(String.class);
            pollenCacheJson = json;
            pollenCachedAt = now;
            return json;
        } catch (Exception e) {
            log.warn("Failed to fetch pollen data: {}", e.getMessage());
            return pollenCacheJson; // return stale cache if available
        }
    }

    private Integer parsePollenValue(JsonNode pollenNode, String key) {
        if (pollenNode == null || pollenNode.isMissingNode()) return null;
        JsonNode keyNode = pollenNode.path(key);
        if (keyNode.isMissingNode()) return null;
        String today = keyNode.path("today").asText("-1");
        try {
            int val = Integer.parseInt(today.trim());
            return val < 0 ? null : val;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
