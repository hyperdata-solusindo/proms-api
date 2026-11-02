package com.hyperdata.proms.module.identity.auth.dto;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;

public record LoginResponse(
        String token, String tokenType, AuthUserDto user, TenantSummaryDto tenant, String workspaceRole) {}
