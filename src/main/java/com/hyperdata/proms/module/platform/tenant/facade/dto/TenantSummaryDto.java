package com.hyperdata.proms.module.platform.tenant.facade.dto;

import java.util.UUID;

public record TenantSummaryDto(UUID id, String slug, String name) {}
