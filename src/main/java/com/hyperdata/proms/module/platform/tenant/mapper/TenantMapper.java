package com.hyperdata.proms.module.platform.tenant.mapper;

import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    TenantDetailDto toDetailDto(Tenant tenant);

    TenantSummaryDto toSummaryDto(Tenant tenant);
}
