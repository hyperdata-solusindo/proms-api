package com.hyperdata.proms.module.identity.auth.facade.dto;

import java.util.Optional;
import java.util.UUID;

public record VerifiedTokenDto(
    Long tokenId,
    UUID userId,
    Optional<UUID> tenantId) {
}
