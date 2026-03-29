package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.CorosStatusDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.CorosToken;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CorosTokenRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Service
public class CorosService {

    private static final Logger log = LoggerFactory.getLogger(CorosService.class);

    private static final String COROS_BASE_URL = "https://open.coros.com";

    @Value("${coros.client-id}")
    private String clientId;

    @Value("${coros.client-secret}")
    private String clientSecret;

    @Value("${coros.redirect-uri}")
    private String redirectUri;

    @Value("${coros.frontend-url}")
    private String frontendUrl;

    private final CorosTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final RestClient restClient;

    public CorosService(CorosTokenRepository tokenRepository, ObjectMapper objectMapper,
                        SecurityUtils securityUtils, AuditLogService auditLogService) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        factory.setReadTimeout(java.time.Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String getAuthorizationUrl() {
        return COROS_BASE_URL + "/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&state=coros";
    }

    public String getFrontendCallbackRedirectUrl() {
        return frontendUrl.replaceAll("/+$", "") + "/overview?coros=connected";
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
                    .uri(COROS_BASE_URL + "/oauth2/accesstoken")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);

            String openId = root.path("openId").asText();
            User currentUser = securityUtils.getCurrentUser();

            CorosToken token = tokenRepository.findByOpenId(openId).orElse(new CorosToken());
            token.setOpenId(openId);
            token.setAccessToken(root.path("access_token").asText());
            token.setRefreshToken(root.path("refresh_token").asText());
            token.setExpiresAt(System.currentTimeMillis() / 1000 + root.path("expires_in").asLong(2592000));
            token.setUser(currentUser);
            tokenRepository.save(token);

            fetchAndStoreUserInfo(token);

            log.info("COROS connected: openId='{}', user={}", openId,
                    currentUser != null ? currentUser.getId() : "null");

            auditLogService.log(currentUser, AuditAction.COROS_CONNECTED, "USER",
                    currentUser != null ? String.valueOf(currentUser.getId()) : null,
                    Map.of("openId", openId));
        } catch (Exception e) {
            log.error("COROS token exchange failed: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange COROS code for token", e);
        }
    }

    private void fetchAndStoreUserInfo(CorosToken token) {
        try {
            String responseBody = restClient.get()
                    .uri(COROS_BASE_URL + "/coros/userinfosim?token=" + token.getAccessToken()
                            + "&openId=" + token.getOpenId())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            if ("0000".equals(root.path("result").asText())) {
                JsonNode data = root.path("data");
                token.setNickname(data.path("nick").asText(null));
                token.setProfilePhoto(data.path("profilePhoto").asText(null));
                tokenRepository.save(token);
            }
        } catch (Exception e) {
            log.warn("Could not fetch COROS user info: {}", e.getMessage());
        }
    }

    public CorosToken refreshTokenIfExpired(CorosToken token) {
        long nowPlusBuffer = System.currentTimeMillis() / 1000 + 86400; // 1 day buffer
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
                    .uri(COROS_BASE_URL + "/oauth2/refresh-token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            if ("0000".equals(root.path("result").asText())) {
                token.setExpiresAt(System.currentTimeMillis() / 1000 + 2592000); // 30 days
                return tokenRepository.save(token);
            }
            log.warn("COROS token refresh returned: {}", root.path("message").asText());
            return token;
        } catch (Exception e) {
            log.error("Failed to refresh COROS token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh COROS token", e);
        }
    }

    public CorosStatusDto getStatus() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return new CorosStatusDto(false, null, null, null);
        }
        Optional<CorosToken> tokenOpt = tokenRepository.findByUserId(currentUser.getId());
        if (tokenOpt.isEmpty()) {
            return new CorosStatusDto(false, null, null, null);
        }
        CorosToken token = tokenOpt.get();
        return new CorosStatusDto(true, token.getNickname(), token.getProfilePhoto(), token.getOpenId());
    }

    public void disconnect() {
        User user = securityUtils.getCurrentUser();
        if (user != null) {
            Optional<CorosToken> tokenOpt = tokenRepository.findByUserId(user.getId());
            if (tokenOpt.isPresent()) {
                CorosToken token = tokenOpt.get();
                deauthorizeOnCoros(token);
                tokenRepository.delete(token);
            }
        }
        auditLogService.log(user, AuditAction.COROS_DISCONNECTED, "USER",
                user != null ? String.valueOf(user.getId()) : null, null);
    }

    private void deauthorizeOnCoros(CorosToken token) {
        try {
            restClient.post()
                    .uri(COROS_BASE_URL + "/oauth2/deauthorize?token=" + token.getAccessToken())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("COROS deauthorization failed (non-critical): {}", e.getMessage());
        }
    }
}
