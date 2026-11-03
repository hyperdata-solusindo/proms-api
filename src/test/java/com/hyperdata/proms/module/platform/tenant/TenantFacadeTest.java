package com.hyperdata.proms.module.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import com.hyperdata.proms.module.platform.tenant.facade.internal.DefaultTenantFacade;
import com.hyperdata.proms.module.platform.tenant.mapper.TenantMapper;
import com.hyperdata.proms.module.platform.tenant.repository.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantFacadeTest {

    @Mock
    private TenantRepository tenantRepository;

    @Spy
    private TenantMapper tenantMapper = Mappers.getMapper(TenantMapper.class);

    @InjectMocks
    private DefaultTenantFacade tenantFacade;

    @Test
    @DisplayName("GIVEN existing tenant ID WHEN getTenantDetail THEN returns mapped TenantDetailDto")
    void givenExistingTenantId_whenGetTenantDetail_thenReturnsDetail() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Hyperdata");
        tenant.setSlug("hyperdata");
        tenant.setStatus("active");
        tenant.setTimezone("Asia/Jakarta");

        given(tenantRepository.findById(tenantId)).willReturn(Optional.of(tenant));

        var result = tenantFacade.getTenantDetail(tenantId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(tenantId);
        assertThat(result.get().name()).isEqualTo("Hyperdata");
        assertThat(result.get().slug()).isEqualTo("hyperdata");
        assertThat(result.get().status()).isEqualTo("active");
        assertThat(result.get().timezone()).isEqualTo("Asia/Jakarta");
    }
}
