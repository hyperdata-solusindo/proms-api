package com.hyperdata.proms.module.platform.tenant.facade.internal;

import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import com.hyperdata.proms.module.platform.tenant.mapper.TenantMapper;
import com.hyperdata.proms.module.platform.tenant.repository.TenantRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultTenantFacade implements TenantFacade {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Override
    public Optional<TenantDetailDto> getTenantDetail(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(tenantMapper::toDetailDto);
    }

    @Override
    public Optional<TenantSummaryDto> getTenantSummary(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(tenantMapper::toSummaryDto);
    }

    @Override
    public List<TenantSummaryDto> getTenantSummaries(Collection<UUID> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }
        return tenantRepository.findAllById(tenantIds).stream()
                .map(tenantMapper::toSummaryDto)
                .toList();
    }

    @Override
    public Map<String, Boolean> getTenantFeatures(UUID tenantId) {
        Map<String, Boolean> features = new HashMap<>();
        features.put("points", true);
        features.put("standup", true);
        return features;
    }
}
