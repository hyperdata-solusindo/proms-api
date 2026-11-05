package com.hyperdata.proms.module.identity.auth.facade;

import com.hyperdata.proms.module.identity.auth.facade.dto.VerifiedTokenDto;
import java.util.Optional;

public interface AuthFacade {

    Optional<VerifiedTokenDto> verifyToken(Long tokenId, String plainToken);

    void recordTokenUsage(Long tokenId);
}
