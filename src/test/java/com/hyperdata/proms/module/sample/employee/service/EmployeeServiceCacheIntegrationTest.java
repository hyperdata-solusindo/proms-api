package com.hyperdata.proms.module.sample.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.common.config.CacheConfig;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.UpdateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapper;
import com.hyperdata.proms.module.sample.employee.repository.EmployeeRepository;
import java.time.LocalDate;
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

@SpringJUnitConfig(classes = {EmployeeServiceCacheIntegrationTest.TestConfig.class})
class EmployeeServiceCacheIntegrationTest {

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        public EmployeeRepository employeeRepository() {
            return mock(EmployeeRepository.class);
        }

        @Bean
        public TenantClient tenantClient() {
            return mock(TenantClient.class);
        }

        @Bean
        public EmployeeMapper employeeMapper() {
            return Mappers.getMapper(EmployeeMapper.class);
        }

        @Bean
        public EmployeeService employeeService(EmployeeRepository employeeRepository,
            EmployeeMapper employeeMapper, TenantClient tenantClient) {
            return new EmployeeService(employeeRepository, employeeMapper, tenantClient);
        }
    }

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TenantClient tenantClient;

    @Test
    @DisplayName("Given an existing employee, when getEmployeeById is called twice, then findById runs once")
    void givenExistingEmployee_whenGetCalledTwice_thenRepositoryQueriedOnce() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Employee employee = Employee.builder()
            .id(id)
            .tenantId(tenantId)
            .employeeNumber("EMP-CACHE-1")
            .fullName("Cached Employee")
            .email("cached.employee@hyperdata.co.id")
            .department("Engineering")
            .position("Backend Engineer")
            .joinDate(LocalDate.parse("2024-01-15"))
            .status(EmployeeStatus.ACTIVE)
            .build();
        TenantSummaryDto tenant = new TenantSummaryDto(
            tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");

        given(employeeRepository.findById(id)).willReturn(Optional.of(employee));
        given(tenantClient.getTenantSummary(tenantId)).willReturn(Optional.of(tenant));

        EmployeeDetailResponse first = employeeService.getEmployeeById(id);
        EmployeeDetailResponse second = employeeService.getEmployeeById(id);

        assertThat(first.tenantName()).isEqualTo("PT Hyperdata Solusindo");
        assertThat(second.fullName()).isEqualTo("Cached Employee");
        verify(employeeRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Given a cached employee, when updateEmployee runs, then the cache is evicted")
    void givenCachedEmployee_whenUpdateCalled_thenCacheIsEvicted() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Employee employee = Employee.builder()
            .id(id)
            .tenantId(tenantId)
            .employeeNumber("EMP-CACHE-2")
            .fullName("Old Name")
            .email("old.name@hyperdata.co.id")
            .department("Engineering")
            .position("Backend Engineer")
            .joinDate(LocalDate.parse("2024-01-15"))
            .status(EmployeeStatus.ACTIVE)
            .build();
        TenantSummaryDto tenant = new TenantSummaryDto(
            tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");

        given(employeeRepository.findById(id)).willReturn(Optional.of(employee));
        given(employeeRepository.save(any(Employee.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(employeeRepository.existsByEmployeeNumberAndIdNot("EMP-CACHE-2", id))
            .willReturn(false);
        given(employeeRepository.existsByEmailAndIdNot("new.name@hyperdata.co.id", id))
            .willReturn(false);
        given(tenantClient.getTenantSummary(tenantId)).willReturn(Optional.of(tenant));

        employeeService.getEmployeeById(id);
        employeeService.updateEmployee(id, new UpdateEmployeeRequest(
            tenantId, "EMP-CACHE-2", "New Name", "new.name@hyperdata.co.id",
            null, "Engineering", "Senior Backend Engineer", EmployeeStatus.ACTIVE));
        employeeService.getEmployeeById(id);

        verify(employeeRepository, times(3)).findById(id);
    }
}
