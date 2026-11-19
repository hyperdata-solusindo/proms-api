package com.hyperdata.proms.module.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import com.hyperdata.proms.module.identity.client.impl.AuthClientImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthClientImplTest {

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthClientImpl client;

    @Test
    @DisplayName("GIVEN valid token with tenant ability WHEN verifyToken THEN returns VerifiedTokenDto with tenantId")
    void givenValidTokenWithTenant_whenVerify_thenReturnsVerifiedTokenDto() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setId(10L);
        pat.setTokenableId(userId);
        pat.setAbilities("tenant:" + tenantId);

        given(tokenService.verifyToken(10L, "raw-token")).willReturn(Optional.of(pat));

        var result = client.verifyToken(10L, "raw-token");

        assertThat(result).isPresent();
        assertThat(result.get().tokenId()).isEqualTo(10L);
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().tenantId()).contains(tenantId);
    }

    @Test
    @DisplayName("GIVEN tokenId WHEN recordTokenUsage THEN delegates to TokenService")
    void givenTokenId_whenRecordUsage_thenDelegatesToTokenService() {
        client.recordTokenUsage(10L);
        verify(tokenService).recordTokenUsage(10L);
    }
}
