package com.trainingsplan.service;

import com.trainingsplan.entity.RefreshToken;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new refresh token for the given user.
     * Returns the raw token string (to be sent to the client).
     */
    @Transactional
    public String createRefreshToken(User user) {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String hash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshExpirationDays);

        RefreshToken refreshToken = new RefreshToken(hash, user, expiresAt);
        repository.save(refreshToken);

        return rawToken;
    }

    /**
     * Validates a refresh token and returns the associated user if valid.
     * The token is consumed (revoked) on successful validation (rotation).
     */
    @Transactional
    public Optional<User> validateAndRotate(String rawToken) {
        String hash = hashToken(rawToken);
        Optional<RefreshToken> tokenOpt = repository.findByTokenHash(hash);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = tokenOpt.get();

        if (token.isRevoked() || token.isExpired()) {
            // If a revoked token is reused, revoke ALL tokens for this user (potential theft)
            if (token.isRevoked()) {
                log.warn("Reuse of revoked refresh token detected for userId={}. Revoking all tokens.", token.getUser().getId());
                repository.revokeAllByUserId(token.getUser().getId());
            }
            return Optional.empty();
        }

        // Revoke the current token (rotation)
        token.setRevoked(true);
        repository.save(token);

        return Optional.of(token.getUser());
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId);
    }

    @Scheduled(fixedRate = 3600_000) // every hour
    @Transactional
    public void cleanupExpired() {
        int deleted = repository.deleteExpiredOrRevoked(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
