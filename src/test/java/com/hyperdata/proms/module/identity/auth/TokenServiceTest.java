package com.hyperdata.proms.module.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private PersonalAccessTokenRepository tokenRepository;

    @Test
    @DisplayName("GIVEN raw plain text WHEN hashToken THEN returns 64-char SHA-256 hex string")
    void givenPlainText_whenHashToken_thenReturnsValidSha256Hex() {
        TokenService service = new TokenService(null);
        String plain = "test-secret-token-12345";
        String hash1 = service.hashToken(plain);
        String hash2 = service.hashToken(plain);

        assertThat(hash1).isNotNull().hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("GIVEN userId and tenantId WHEN createToken THEN persists token with configured TTL")
    void givenUserAndTenant_whenCreateToken_thenPersistsWithTtl() {
        TokenService service = new TokenService(tokenRepository, 30, 7);

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        given(tokenRepository.save(any(PersonalAccessToken.class))).willAnswer(invocation -> {
            PersonalAccessToken arg = invocation.getArgument(0);
            arg.setId(10L);
            return arg;
        });

        String result = service.createToken(userId, tenantId);

        assertThat(result).startsWith("10|");

        ArgumentCaptor<PersonalAccessToken> captor = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(tokenRepository).save(captor.capture());

        PersonalAccessToken saved = captor.getValue();
        assertThat(saved.getTokenableId()).isEqualTo(userId);
        assertThat(saved.getAbilities()).isEqualTo("tenant:" + tenantId);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("GIVEN token with remaining TTL > slidingWindow WHEN recordTokenUsage THEN does NOT extend expiresAt")
    void givenAmpleRemainingTtl_whenRecordUsage_thenExpiresAtUnchanged() {
        TokenService service = new TokenService(tokenRepository, 30, 7);

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setId(1L);
        Instant initialExpiry = Instant.now().plus(20, ChronoUnit.DAYS);
        pat.setExpiresAt(initialExpiry);

        given(tokenRepository.findById(1L)).willReturn(Optional.of(pat));

        service.recordTokenUsage(1L);

        assertThat(pat.getLastUsedAt()).isNotNull();
        assertThat(pat.getExpiresAt()).isEqualTo(initialExpiry);
        verify(tokenRepository).save(pat);
    }

    @Test
    @DisplayName("GIVEN token with remaining TTL < slidingWindow WHEN recordTokenUsage THEN extends expiresAt by TTL")
    void givenLowRemainingTtl_whenRecordUsage_thenExtendsExpiresAt() {
        TokenService service = new TokenService(tokenRepository, 30, 7);

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setId(1L);
        // Sisa waktu hanya 3 hari (di bawah threshold 7 hari)
        Instant initialExpiry = Instant.now().plus(3, ChronoUnit.DAYS);
        pat.setExpiresAt(initialExpiry);

        given(tokenRepository.findById(1L)).willReturn(Optional.of(pat));

        service.recordTokenUsage(1L);

        assertThat(pat.getLastUsedAt()).isNotNull();
        // expiresAt harus digeser menjadi ~30 hari dari sekarang
        assertThat(pat.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
        verify(tokenRepository).save(pat);
    }

    @Test
    @DisplayName("GIVEN expired token WHEN verifyToken THEN returns empty")
    void givenExpiredToken_whenVerifyToken_thenReturnsEmpty() {
        TokenService service = new TokenService(tokenRepository, 30, 7);

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setId(1L);
        pat.setToken(service.hashToken("secret-token"));
        pat.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS)); // sudah lewat

        given(tokenRepository.findByIdAndToken(1L, service.hashToken("secret-token")))
                .willReturn(Optional.of(pat));

        Optional<PersonalAccessToken> result = service.verifyToken(1L, "secret-token");

        assertThat(result).isEmpty();
    }
}
