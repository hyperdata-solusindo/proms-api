package com.hyperdata.proms.module.sample.employee.mapper;

import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import com.hyperdata.proms.module.sample.employee.dto.CreateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeListItemResponse;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.client.dto.EmployeeSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    @Mapping(target = "id", source = "employee.id")
    @Mapping(target = "tenantId", source = "employee.tenantId")
    @Mapping(target = "tenantName", source = "tenant.name")
    @Mapping(target = "tenantSlug", source = "tenant.slug")
    EmployeeDetailResponse toDetailResponse(Employee employee, TenantSummaryDto tenant);

    @Mapping(target = "id", source = "employee.id")
    @Mapping(target = "tenantId", source = "employee.tenantId")
    @Mapping(target = "tenantName", source = "tenant.name")
    EmployeeListItemResponse toListItemResponse(Employee employee, TenantSummaryDto tenant);

    EmployeeSummaryDto toSummaryDto(Employee employee);
}
