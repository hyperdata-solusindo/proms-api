package com.hyperdata.proms.module.platform.client.contract;

import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TenantClient {

    Optional<TenantDetailDto> getTenantDetail(UUID tenantId);

    Optional<TenantSummaryDto> getTenantSummary(UUID tenantId);

    List<TenantSummaryDto> getTenantSummaries(Collection<UUID> tenantIds);

    Map<String, Boolean> getTenantFeatures(UUID tenantId);
}
