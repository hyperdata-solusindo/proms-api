package com.hyperdata.proms.common.security;

import java.util.UUID;

public record CurrentUser(UUID userId, String email, UUID activeTenantId, String workspaceRole, Long tokenId) {
}
