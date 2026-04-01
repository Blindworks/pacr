package com.trainingsplan.service;

import com.trainingsplan.entity.BlacklistedToken;
import com.trainingsplan.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final BlacklistedTokenRepository repository;

    public TokenBlacklistService(BlacklistedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void blacklist(String token, Date expiration) {
        String hash = hashToken(token);
        if (repository.existsByTokenHash(hash)) {
            return;
        }
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                expiration.toInstant(), ZoneId.systemDefault());
        repository.save(new BlacklistedToken(hash, expiresAt));
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByTokenHash(hashToken(token));
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpired() {
        int deleted = repository.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired blacklisted tokens", deleted);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
