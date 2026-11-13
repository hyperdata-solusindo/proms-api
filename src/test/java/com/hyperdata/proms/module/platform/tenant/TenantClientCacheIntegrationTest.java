package com.hyperdata.proms.module.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.common.config.CacheConfig;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.impl.TenantClientImpl;
import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import com.hyperdata.proms.module.platform.tenant.mapper.TenantMapper;
import com.hyperdata.proms.module.platform.tenant.repository.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {TenantClientCacheIntegrationTest.TestConfig.class})
class TenantClientCacheIntegrationTest {

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        public TenantRepository tenantRepository() {
            return mock(TenantRepository.class);
        }

        @Bean
        public TenantMapper tenantMapper() {
            return Mappers.getMapper(TenantMapper.class);
        }

        @Bean
        public TenantClient tenantClient(TenantRepository tenantRepository, TenantMapper tenantMapper) {
            return new TenantClientImpl(tenantRepository, tenantMapper);
        }
    }

    @Autowired
    private TenantClient tenantClient;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("Given cached tenant client, when getTenantDetail called twice, then repository is queried only once")
    void givenCachedTenantClient_whenGetTenantDetailCalledTwice_thenRepositoryQueriedOnlyOnce() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Hyperdata");
        tenant.setSlug("hyperdata");
        tenant.setStatus("active");
        tenant.setTimezone("Asia/Jakarta");

        given(tenantRepository.findById(tenantId)).willReturn(Optional.of(tenant));

        // First call - queries repository and populates L1 cache
        var firstResult = tenantClient.getTenantDetail(tenantId);
        assertThat(firstResult).isPresent();
        assertThat(firstResult.get().name()).isEqualTo("Hyperdata");

        // Second call - served from L1 Caffeine cache
        var secondResult = tenantClient.getTenantDetail(tenantId);
        assertThat(secondResult).isPresent();
        assertThat(secondResult.get().name()).isEqualTo("Hyperdata");

        // Verify repository was only hit once
        verify(tenantRepository, times(1)).findById(tenantId);
    }

    @Test
    @DisplayName("Given non-existing tenant, when getTenantDetail called, then returns empty optional and is not cached")
    void givenNonExistingTenant_whenGetTenantDetailCalled_thenReturnsEmptyOptionalAndNotCached() {
        UUID tenantId = UUID.randomUUID();
        given(tenantRepository.findById(tenantId)).willReturn(Optional.empty());

        var firstResult = tenantClient.getTenantDetail(tenantId);
        assertThat(firstResult).isEmpty();

        var secondResult = tenantClient.getTenantDetail(tenantId);
        assertThat(secondResult).isEmpty();

        // Repository is queried each time because empty results are excluded via unless="#result == null"
        verify(tenantRepository, times(2)).findById(tenantId);
    }

    @Test
    @DisplayName("Given tenant features requested, when getTenantFeatures called twice, then cached properly")
    void givenTenantFeaturesRequested_whenGetTenantFeaturesCalledTwice_thenCachedProperly() {
        UUID tenantId = UUID.randomUUID();

        var features1 = tenantClient.getTenantFeatures(tenantId);
        var features2 = tenantClient.getTenantFeatures(tenantId);

        assertThat(features1).isEqualTo(features2);
        assertThat(features1).containsEntry("points", true);
    }
}
