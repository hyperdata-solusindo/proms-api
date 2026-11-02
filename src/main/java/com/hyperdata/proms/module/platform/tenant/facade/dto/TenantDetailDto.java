package com.hyperdata.proms.module.platform.tenant.facade.dto;

import java.util.UUID;

public record TenantDetailDto(UUID id, String slug, String name, String status, String timezone) {}
