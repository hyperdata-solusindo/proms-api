package com.hyperdata.proms.module.identity.auth.service;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final PersonalAccessTokenRepository tokenRepository;
    private final int tokenTtlDays;
    private final int slidingWindowDays;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public TokenService(
        PersonalAccessTokenRepository tokenRepository,
        @Value("${app.security.token.ttl-days:30}")
        int tokenTtlDays,
        @Value("${app.security.token.sliding-window-days:7}")
        int slidingWindowDays) {
        this.tokenRepository = tokenRepository;
        this.tokenTtlDays = tokenTtlDays;
        this.slidingWindowDays = slidingWindowDays;
    }

    public TokenService(PersonalAccessTokenRepository tokenRepository) {
        this(tokenRepository, 30, 7);
    }

    @Transactional
    public String createToken(UUID userId, UUID tenantId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String plainToken = HexFormat.of().formatHex(randomBytes);
        String hashedToken = hashToken(plainToken);

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setTokenableType("User");
        pat.setTokenableId(userId);
        pat.setName("auth-session");
        pat.setToken(hashedToken);
        pat.setAbilities("tenant:" + tenantId);
        pat.setExpiresAt(Instant.now().plus(tokenTtlDays, ChronoUnit.DAYS));

        PersonalAccessToken saved = tokenRepository.save(pat);
        return saved.getId() + "|" + plainToken;
    }

    @Transactional(readOnly = true)
    public Optional<PersonalAccessToken> verifyToken(Long tokenId, String plainToken) {
        String hashed = hashToken(plainToken);
        return tokenRepository
            .findByIdAndToken(tokenId, hashed)
            .filter(pat -> pat.getExpiresAt() == null || pat.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void recordTokenUsage(Long tokenId) {
        tokenRepository.findById(tokenId).ifPresent(pat -> {
            Instant now = Instant.now();
            pat.setLastUsedAt(now);

            // Sliding Expiration: Perpanjang masa berlaku jika sisa waktu kurang dari slidingWindowDays
            if (pat.getExpiresAt() != null) {
                Instant threshold = now.plus(slidingWindowDays, ChronoUnit.DAYS);
                if (pat.getExpiresAt().isBefore(threshold)) {
                    pat.setExpiresAt(now.plus(tokenTtlDays, ChronoUnit.DAYS));
                }
            }

            tokenRepository.save(pat);
        });
    }

    public String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
