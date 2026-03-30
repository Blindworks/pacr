package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.YolandaStatusDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.BodyMeasurement;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.YolandaToken;
import com.trainingsplan.repository.BodyMeasurementRepository;
import com.trainingsplan.repository.YolandaTokenRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class YolandaService {

    private static final Logger log = LoggerFactory.getLogger(YolandaService.class);

    /**
     * Base URL for the Yolanda Open API.
     * This is a placeholder — update once Yolanda provides the actual API base URL
     * after the cooperation agreement is approved.
     */
    private static final String YOLANDA_BASE_URL = "https://api.yolanda.hk";

    @Value("${yolanda.client-id}")
    private String clientId;

    @Value("${yolanda.client-secret}")
    private String clientSecret;

    @Value("${yolanda.redirect-uri}")
    private String redirectUri;

    @Value("${yolanda.frontend-url}")
    private String frontendUrl;

    private final YolandaTokenRepository tokenRepository;
    private final BodyMeasurementRepository bodyMeasurementRepository;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final RestClient restClient;

    public YolandaService(YolandaTokenRepository tokenRepository,
                          BodyMeasurementRepository bodyMeasurementRepository,
                          ObjectMapper objectMapper,
                          SecurityUtils securityUtils,
                          AuditLogService auditLogService) {
        this.tokenRepository = tokenRepository;
        this.bodyMeasurementRepository = bodyMeasurementRepository;
        this.objectMapper = objectMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        factory.setReadTimeout(java.time.Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String getAuthorizationUrl() {
        return YOLANDA_BASE_URL + "/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&state=yolanda";
    }

    public String getFrontendCallbackRedirectUrl() {
        return frontendUrl.replaceAll("/+$", "") + "/settings?yolanda=connected";
    }

    public void exchangeCodeForToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "authorization_code");

        try {
            String responseBody = restClient.post()
                    .uri(YOLANDA_BASE_URL + "/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);

            String accountId = root.path("account_id").asText(
                    root.path("user_id").asText(root.path("openId").asText()));
            User currentUser = securityUtils.getCurrentUser();

            YolandaToken token = tokenRepository.findByAccountId(accountId).orElse(new YolandaToken());
            token.setAccountId(accountId);
            token.setAccessToken(root.path("access_token").asText());
            token.setRefreshToken(root.path("refresh_token").asText());
            token.setExpiresAt(System.currentTimeMillis() / 1000 + root.path("expires_in").asLong(2592000));
            token.setUser(currentUser);

            if (root.has("nickname")) {
                token.setNickname(root.path("nickname").asText(null));
            }

            tokenRepository.save(token);

            log.info("Yolanda connected: accountId='{}', user={}", accountId,
                    currentUser != null ? currentUser.getId() : "null");

            auditLogService.log(currentUser, AuditAction.YOLANDA_CONNECTED, "USER",
                    currentUser != null ? String.valueOf(currentUser.getId()) : null,
                    Map.of("accountId", accountId));
        } catch (Exception e) {
            log.error("Yolanda token exchange failed: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange Yolanda code for token", e);
        }
    }

    public YolandaToken refreshTokenIfExpired(YolandaToken token) {
        long nowPlusBuffer = System.currentTimeMillis() / 1000 + 86400;
        if (token.getExpiresAt() != null && token.getExpiresAt() > nowPlusBuffer) {
            return token;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("refresh_token", token.getRefreshToken());
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "refresh_token");

        try {
            String responseBody = restClient.post()
                    .uri(YOLANDA_BASE_URL + "/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            token.setAccessToken(root.path("access_token").asText());
            if (root.has("refresh_token")) {
                token.setRefreshToken(root.path("refresh_token").asText());
            }
            token.setExpiresAt(System.currentTimeMillis() / 1000 + root.path("expires_in").asLong(2592000));
            return tokenRepository.save(token);
        } catch (Exception e) {
            log.error("Failed to refresh Yolanda token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh Yolanda token", e);
        }
    }

    public YolandaStatusDto getStatus() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return new YolandaStatusDto(false, null, null);
        }
        Optional<YolandaToken> tokenOpt = tokenRepository.findByUserId(currentUser.getId());
        if (tokenOpt.isEmpty()) {
            return new YolandaStatusDto(false, null, null);
        }
        YolandaToken token = tokenOpt.get();
        return new YolandaStatusDto(true, token.getNickname(), token.getAccountId());
    }

    public void disconnect() {
        User user = securityUtils.getCurrentUser();
        if (user != null) {
            Optional<YolandaToken> tokenOpt = tokenRepository.findByUserId(user.getId());
            tokenOpt.ifPresent(tokenRepository::delete);
        }
        auditLogService.log(user, AuditAction.YOLANDA_DISCONNECTED, "USER",
                user != null ? String.valueOf(user.getId()) : null, null);
    }

    /**
     * Sync body measurements from Yolanda for the given date range.
     * Returns the list of newly created or updated BodyMeasurement entries.
     *
     * NOTE: The exact API endpoint and response format are placeholders.
     * These will be updated once the Yolanda Open API cooperation agreement is approved
     * and the actual API documentation is available.
     */
    @Transactional
    public List<BodyMeasurement> syncMeasurements(LocalDate from, LocalDate to) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("Not authenticated");
        }

        YolandaToken token = tokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Yolanda not connected"));
        token = refreshTokenIfExpired(token);

        try {
            String responseBody = restClient.get()
                    .uri(YOLANDA_BASE_URL + "/api/measurements?from=" +
                            from.format(DateTimeFormatter.ISO_LOCAL_DATE) +
                            "&to=" + to.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .header("Authorization", "Bearer " + token.getAccessToken())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode measurements = root.path("data").isArray() ? root.path("data") : root;

            List<BodyMeasurement> synced = new ArrayList<>();

            for (JsonNode node : measurements) {
                String externalId = node.path("id").asText(node.path("measurement_id").asText(null));
                if (externalId == null) continue;

                Optional<BodyMeasurement> existing = bodyMeasurementRepository
                        .findByExternalIdAndUserId(externalId, user.getId());

                BodyMeasurement bm = existing.orElseGet(BodyMeasurement::new);
                bm.setUser(user);
                bm.setSource("YOLANDA");
                bm.setExternalId(externalId);

                if (node.has("timestamp")) {
                    bm.setMeasuredAt(LocalDate.parse(node.path("timestamp").asText().substring(0, 10)));
                } else if (node.has("date")) {
                    bm.setMeasuredAt(LocalDate.parse(node.path("date").asText()));
                } else if (node.has("measured_at")) {
                    bm.setMeasuredAt(LocalDate.parse(node.path("measured_at").asText().substring(0, 10)));
                }

                if (node.has("weight")) bm.setWeightKg(node.path("weight").asDouble());
                if (node.has("bmi")) bm.setBmi(node.path("bmi").asDouble());
                if (node.has("body_fat") || node.has("bodyfat")) {
                    bm.setFatPercentage(node.has("body_fat") ?
                            node.path("body_fat").asDouble() : node.path("bodyfat").asDouble());
                }
                if (node.has("water") || node.has("body_water")) {
                    bm.setWaterPercentage(node.has("water") ?
                            node.path("water").asDouble() : node.path("body_water").asDouble());
                }
                if (node.has("muscle") || node.has("muscle_mass")) {
                    bm.setMuscleMassKg(node.has("muscle") ?
                            node.path("muscle").asDouble() : node.path("muscle_mass").asDouble());
                }
                if (node.has("bone") || node.has("bone_mass")) {
                    bm.setBoneMassKg(node.has("bone") ?
                            node.path("bone").asDouble() : node.path("bone_mass").asDouble());
                }
                if (node.has("visceral_fat")) {
                    bm.setVisceralFatLevel(node.path("visceral_fat").asInt());
                }
                if (node.has("metabolic_age")) {
                    bm.setMetabolicAge(node.path("metabolic_age").asInt());
                }

                synced.add(bodyMeasurementRepository.save(bm));
            }

            log.info("Yolanda sync: userId={}, range={} to {}, synced={} measurements",
                    user.getId(), from, to, synced.size());

            auditLogService.log(user, AuditAction.YOLANDA_SYNC, "USER",
                    String.valueOf(user.getId()),
                    Map.of("from", from.toString(), "to", to.toString(), "count", String.valueOf(synced.size())));

            return synced;
        } catch (Exception e) {
            log.error("Yolanda sync failed for user {}: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Failed to sync measurements from Yolanda", e);
        }
    }
}
