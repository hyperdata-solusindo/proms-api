package com.hyperdata.proms.module.sample.employee.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EmployeeMapperTest {

    private final EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

    private Employee employee(UUID tenantId) {
        return Employee.builder()
            .id(UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"))
            .tenantId(tenantId)
            .employeeNumber("EMP-0001")
            .fullName("Budi Santoso")
            .email("budi.santoso@hyperdata.co.id")
            .phone("+6281234567890")
            .department("Engineering")
            .position("Backend Engineer")
            .joinDate(LocalDate.parse("2024-01-15"))
            .status(EmployeeStatus.ACTIVE)
            .build();
    }

    @Test
    @DisplayName("Given tenant summary, when toDetailResponse, then tenant fields are populated")
    void shouldMapTenantIntoDetail() {
        UUID tenantId = UUID.randomUUID();
        TenantSummaryDto tenant = new TenantSummaryDto(
            tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");

        EmployeeDetailResponse response = mapper.toDetailResponse(employee(tenantId), tenant);

        assertEquals("PT Hyperdata Solusindo", response.tenantName());
        assertEquals("hyperdata-solusindo", response.tenantSlug());
    }

    @Test
    @DisplayName("Given null tenant, when toDetailResponse, then tenant fields are null")
    void shouldMapNullTenantSafely() {
        EmployeeDetailResponse response = mapper.toDetailResponse(employee(UUID.randomUUID()), null);

        assertNull(response.tenantName());
        assertNull(response.tenantSlug());
    }
}
