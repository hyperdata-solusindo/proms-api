package com.hyperdata.proms.module.identity.auth.facade.internal;

import com.hyperdata.proms.module.identity.auth.facade.AuthFacade;
import com.hyperdata.proms.module.identity.auth.facade.dto.VerifiedTokenDto;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAuthFacade implements AuthFacade {

    private final TokenService tokenService;

    @Override
    public Optional<VerifiedTokenDto> verifyToken(Long tokenId, String plainToken) {
        return tokenService.verifyToken(tokenId, plainToken).map(pat -> {
            UUID tenantId = null;
            if (pat.getAbilities() != null && pat.getAbilities().startsWith("tenant:")) {
                try {
                    tenantId = UUID.fromString(pat.getAbilities().substring(7));
                } catch (IllegalArgumentException ignored) {
                    // Invalid tenant UUID in abilities
                }
            }
            return new VerifiedTokenDto(pat.getId(), pat.getTokenableId(), Optional.ofNullable(tenantId));
        });
    }

    @Override
    public void recordTokenUsage(Long tokenId) {
        tokenService.recordTokenUsage(tokenId);
    }
}
