package com.hyperdata.proms.module.sample.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyperdata.proms.module.sample.client.dto.EmployeeSummaryDto;
import com.hyperdata.proms.module.sample.client.impl.EmployeeClientImpl;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapper;
import com.hyperdata.proms.module.sample.employee.repository.EmployeeRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeClientImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeClientImpl employeeClient;

    @Test
    @DisplayName("Given existing ID, when findEmployeeSummaryById, then return the summary")
    void shouldReturnSummaryWhenFound() {
        UUID id = UUID.randomUUID();
        Employee employee = Employee.builder()
            .id(id)
            .employeeNumber("EMP-0001")
            .fullName("Budi Santoso")
            .department("Engineering")
            .status(EmployeeStatus.ACTIVE)
            .tenantId(UUID.randomUUID())
            .build();
        EmployeeSummaryDto summary = new EmployeeSummaryDto(
            id, "EMP-0001", "Budi Santoso", "Engineering", EmployeeStatus.ACTIVE,
            employee.getTenantId());

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));
        when(employeeMapper.toSummaryDto(employee)).thenReturn(summary);

        Optional<EmployeeSummaryDto> result = employeeClient.findEmployeeSummaryById(id);

        assertTrue(result.isPresent());
        assertEquals("Budi Santoso", result.get().fullName());
    }

    @Test
    @DisplayName("Given unknown ID, when findEmployeeSummaryById, then return empty")
    void shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());
        assertTrue(employeeClient.findEmployeeSummaryById(id).isEmpty());
    }

    @Test
    @DisplayName("Given batch IDs, when findAllByIds, then map every entity")
    void shouldReturnSummariesForBatchIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Employee first = Employee.builder()
            .id(id1)
            .employeeNumber("EMP-0001")
            .fullName("Budi Santoso")
            .build();
        Employee second = Employee.builder()
            .id(id2)
            .employeeNumber("EMP-0002")
            .fullName("Siti Aminah")
            .build();
        EmployeeSummaryDto firstDto = new EmployeeSummaryDto(
            id1, "EMP-0001", "Budi Santoso", "Engineering", EmployeeStatus.ACTIVE, null);
        EmployeeSummaryDto secondDto = new EmployeeSummaryDto(
            id2, "EMP-0002", "Siti Aminah", "Finance", EmployeeStatus.ACTIVE, null);

        when(employeeRepository.findAllById(Set.of(id1, id2))).thenReturn(List.of(first, second));
        when(employeeMapper.toSummaryDto(first)).thenReturn(firstDto);
        when(employeeMapper.toSummaryDto(second)).thenReturn(secondDto);

        List<EmployeeSummaryDto> result = employeeClient.findAllByIds(Set.of(id1, id2));

        assertEquals(2, result.size());
        assertEquals("Budi Santoso", result.get(0).fullName());
        assertEquals("Siti Aminah", result.get(1).fullName());
    }

    @Test
    @DisplayName("Given null or empty IDs, when findAllByIds, then return empty without querying")
    void shouldReturnEmptyListWhenIdsNullOrEmpty() {
        assertTrue(employeeClient.findAllByIds(null).isEmpty());
        assertTrue(employeeClient.findAllByIds(Collections.emptyList()).isEmpty());
        verify(employeeRepository, never()).findAllById(any());
    }
}
