package com.hyperdata.proms.module.identity.client.contract;

import com.hyperdata.proms.module.identity.client.dto.VerifiedTokenDto;
import java.util.Optional;

public interface AuthClient {

    Optional<VerifiedTokenDto> verifyToken(Long tokenId, String plainToken);

    void recordTokenUsage(Long tokenId);
}
