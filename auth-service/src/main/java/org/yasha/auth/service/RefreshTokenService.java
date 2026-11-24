package org.yasha.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yasha.auth.entity.RefreshToken;
import org.yasha.auth.exception.InvalidTokenException;
import org.yasha.auth.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;

    @Transactional
    public String create(UUID userId) {
        String raw = generateRaw();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(OffsetDateTime.now().plus(TTL));
        repository.save(token);
        return raw;
    }

    @Transactional
    public UUID validateAndRotate(String rawToken) {
        RefreshToken token = repository.findByTokenHashAndRevokedAtIsNull(sha256(rawToken))
                .orElseThrow(() -> new InvalidTokenException("refresh token invalid or revoked"));
        if (!token.isActive()) {
            throw new InvalidTokenException("refresh token expired");
        }
        token.setRevokedAt(OffsetDateTime.now());
        repository.save(token);
        return token.getUserId();
    }

    @Transactional
    public void revoke(UUID userId, String rawToken) {
        repository.findByTokenHashAndUserIdAndRevokedAtIsNull(sha256(rawToken), userId)
                .ifPresent(token -> {
                    token.setRevokedAt(OffsetDateTime.now());
                    repository.save(token);
                });
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        repository.findByTokenHashAndRevokedAtIsNull(sha256(rawToken))
                .ifPresent(token -> {
                    token.setRevokedAt(OffsetDateTime.now());
                    repository.save(token);
                });
    }

    @Transactional
    public void revokeAllByUser(UUID userId) {
        repository.revokeAllByUser(userId, OffsetDateTime.now());
    }

    private String generateRaw() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}