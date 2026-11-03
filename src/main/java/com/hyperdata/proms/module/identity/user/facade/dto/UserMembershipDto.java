package com.hyperdata.proms.module.identity.user.facade.dto;

import java.util.UUID;

public record UserMembershipDto(UUID tenantId, String role) {
}
