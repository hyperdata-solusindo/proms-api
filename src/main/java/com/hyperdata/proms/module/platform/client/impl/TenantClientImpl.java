package com.hyperdata.proms.module.platform.client.impl;

import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import com.hyperdata.proms.module.platform.tenant.constant.TenantCacheNames;
import com.hyperdata.proms.module.platform.tenant.mapper.TenantMapper;
import com.hyperdata.proms.module.platform.tenant.repository.TenantRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantClientImpl implements TenantClient {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Override
    @Cacheable(cacheNames = TenantCacheNames.TENANT_DETAILS, key = "#tenantId", unless = "#result == null")
    public Optional<TenantDetailDto> getTenantDetail(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(tenantMapper::toDetailDto);
    }

    @Override
    @Cacheable(cacheNames = TenantCacheNames.TENANT_SUMMARIES, key = "#tenantId", unless = "#result == null")
    public Optional<TenantSummaryDto> getTenantSummary(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(tenantMapper::toSummaryDto);
    }

    @Override
    public List<TenantSummaryDto> getTenantSummaries(Collection<UUID> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }
        return tenantRepository.findAllById(tenantIds)
            .stream()
            .map(tenantMapper::toSummaryDto)
            .toList();
    }

    @Override
    @Cacheable(cacheNames = TenantCacheNames.TENANT_FEATURES, key = "#tenantId")
    public Map<String, Boolean> getTenantFeatures(UUID tenantId) {
        Map<String, Boolean> features = new HashMap<>();
        features.put("points", true);
        features.put("standup", true);
        return features;
    }
}
