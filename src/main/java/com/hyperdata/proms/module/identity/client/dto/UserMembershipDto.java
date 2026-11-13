package com.hyperdata.proms.module.identity.client.dto;

import java.util.UUID;

public record UserMembershipDto(UUID tenantId, String role) {
}
