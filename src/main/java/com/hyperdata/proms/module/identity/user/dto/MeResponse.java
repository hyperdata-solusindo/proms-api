package com.hyperdata.proms.module.identity.user.dto;

import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import java.util.List;
import java.util.Map;

public record MeResponse(
    UserProfileDto user,
    TenantDetailDto tenant,
    String workspaceRole,
    Map<String, Boolean> features,
    List<OtherTenantDto> otherTenants) {
}
