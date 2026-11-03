package com.hyperdata.proms.module.platform.tenant.facade;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TenantFacade {

    Optional<TenantDetailDto> getTenantDetail(UUID tenantId);

    Optional<TenantSummaryDto> getTenantSummary(UUID tenantId);

    List<TenantSummaryDto> getTenantSummaries(Collection<UUID> tenantIds);

    Map<String, Boolean> getTenantFeatures(UUID tenantId);
}
