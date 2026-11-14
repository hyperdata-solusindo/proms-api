package com.hyperdata.proms.module.sample.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import com.hyperdata.proms.module.sample.employee.dto.CreateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeListItemResponse;
import com.hyperdata.proms.module.sample.employee.dto.UpdateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapper;
import com.hyperdata.proms.module.sample.employee.repository.EmployeeRepository;
import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22");
    private static final TenantSummaryDto TENANT = new TenantSummaryDto(
        TENANT_ID, "hyperdata-solusindo", "PT Hyperdata Solusindo");

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private TenantClient tenantClient;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee() {
        return Employee.builder()
            .id(UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"))
            .tenantId(TENANT_ID)
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

    private CreateEmployeeRequest createRequest() {
        return new CreateEmployeeRequest(
            TENANT_ID, "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Backend Engineer",
            LocalDate.parse("2024-01-15"), EmployeeStatus.ACTIVE);
    }

    private UpdateEmployeeRequest updateRequest() {
        return new UpdateEmployeeRequest(
            TENANT_ID, "EMP-0001", "Budi Santoso Updated", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Senior Backend Engineer", EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("Given a valid request, when createEmployee, then save and return detail")
    void shouldCreateEmployeeSuccessfully() {
        CreateEmployeeRequest request = createRequest();
        Employee entity = employee();
        EmployeeDetailResponse detail = new EmployeeDetailResponse(
            entity.getId(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE, TENANT_ID, TENANT.name(), TENANT.slug(), null);

        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeRepository.existsByEmployeeNumber("EMP-0001")).thenReturn(false);
        when(employeeRepository.existsByEmail("budi.santoso@hyperdata.co.id")).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDetailResponse(entity, TENANT)).thenReturn(detail);

        EmployeeDetailResponse result = employeeService.createEmployee(request);

        assertEquals(detail, result);
        verify(employeeRepository).save(entity);
    }

    @Test
    @DisplayName("Given an existing employee number, when createEmployee, then throw duplicate")
    void shouldThrowWhenEmployeeNumberExists() {
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeRepository.existsByEmployeeNumber("EMP-0001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(createRequest()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given an existing email, when createEmployee, then throw duplicate")
    void shouldThrowWhenEmailExists() {
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeRepository.existsByEmployeeNumber("EMP-0001")).thenReturn(false);
        when(employeeRepository.existsByEmail("budi.santoso@hyperdata.co.id")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(createRequest()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given an unknown tenant, when createEmployee, then throw not found")
    void shouldThrowWhenTenantNotFoundOnCreate() {
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(createRequest()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given an existing employee, when getEmployeeById, then return detail with tenant")
    void shouldReturnDetailWithTenantWhenFound() {
        Employee entity = employee();
        EmployeeDetailResponse detail = new EmployeeDetailResponse(
            entity.getId(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE, TENANT_ID, TENANT.name(), TENANT.slug(), null);

        when(employeeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeMapper.toDetailResponse(entity, TENANT)).thenReturn(detail);

        EmployeeDetailResponse result = employeeService.getEmployeeById(entity.getId());

        assertEquals(TENANT.name(), result.tenantName());
    }

    @Test
    @DisplayName("Given an unknown employee, when getEmployeeById, then throw not found")
    void shouldThrowWhenEmployeeNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(id));
    }

    @Test
    @DisplayName("Given a valid update, when updateEmployee, then copy fields and return detail")
    void shouldUpdateEmployeeSuccessfully() {
        Employee entity = employee();
        UpdateEmployeeRequest request = updateRequest();
        EmployeeDetailResponse detail = new EmployeeDetailResponse(
            entity.getId(), "EMP-0001", "Budi Santoso Updated", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Senior Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE, TENANT_ID, TENANT.name(), TENANT.slug(), null);

        when(employeeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeRepository.existsByEmployeeNumberAndIdNot("EMP-0001", entity.getId())).thenReturn(false);
        when(employeeRepository.existsByEmailAndIdNot("budi.santoso@hyperdata.co.id", entity.getId()))
            .thenReturn(false);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDetailResponse(entity, TENANT)).thenReturn(detail);

        EmployeeDetailResponse result = employeeService.updateEmployee(entity.getId(), request);

        assertEquals("Budi Santoso Updated", entity.getFullName());
        assertEquals("Senior Backend Engineer", entity.getPosition());
        assertEquals(detail, result);
    }

    @Test
    @DisplayName("Given another employee with the number, when updateEmployee, then throw duplicate")
    void shouldThrowWhenEmployeeNumberExistsOnUpdate() {
        Employee entity = employee();
        when(employeeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeRepository.existsByEmployeeNumberAndIdNot("EMP-0001", entity.getId())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> employeeService.updateEmployee(entity.getId(), updateRequest()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given another employee with the email, when updateEmployee, then throw duplicate")
    void shouldThrowWhenEmailExistsOnUpdate() {
        Employee entity = employee();
        when(employeeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(tenantClient.getTenantSummary(TENANT_ID)).thenReturn(Optional.of(TENANT));
        when(employeeRepository.existsByEmployeeNumberAndIdNot("EMP-0001", entity.getId())).thenReturn(false);
        when(employeeRepository.existsByEmailAndIdNot("budi.santoso@hyperdata.co.id", entity.getId())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> employeeService.updateEmployee(entity.getId(), updateRequest()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a tenant missing from the batch result, when getEmployees, then tenantName is null")
    void shouldReturnListWithNullTenantWhenTenantMissing() {
        Employee entity = employee();
        Page<Employee> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        EmployeeListItemResponse item = new EmployeeListItemResponse(
            entity.getId(), "EMP-0001", "Budi Santoso", "Engineering",
            "Backend Engineer", EmployeeStatus.ACTIVE, entity.getTenantId(), null);

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(tenantClient.getTenantSummaries(Set.of(entity.getTenantId()))).thenReturn(List.of());
        when(employeeMapper.toListItemResponse(entity, null)).thenReturn(item);

        PageResponse<EmployeeListItemResponse> result = employeeService.getEmployees(
            new EmployeeFilter(null, null, null, null, null), PageRequest.of(0, 20));

        assertNull(result.items().get(0).tenantName());
    }

    @Test
    @DisplayName("Given a non-empty page, when getEmployees, then tenant batch is called exactly once")
    void shouldCallTenantBatchOnceForList() {
        Employee entity = employee();
        Page<Employee> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        EmployeeListItemResponse item = new EmployeeListItemResponse(
            entity.getId(), "EMP-0001", "Budi Santoso", "Engineering",
            "Backend Engineer", EmployeeStatus.ACTIVE, entity.getTenantId(), TENANT.name());

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(tenantClient.getTenantSummaries(Set.of(entity.getTenantId()))).thenReturn(List.of(TENANT));
        when(employeeMapper.toListItemResponse(entity, TENANT)).thenReturn(item);

        PageResponse<EmployeeListItemResponse> result = employeeService.getEmployees(
            new EmployeeFilter(null, null, null, null, null), PageRequest.of(0, 20));

        assertEquals(1, result.items().size());
        assertEquals("PT Hyperdata Solusindo", result.items().get(0).tenantName());
        verify(tenantClient, times(1)).getTenantSummaries(Set.of(entity.getTenantId()));
        verify(tenantClient, never()).getTenantSummary(any());
    }

    @Test
    @DisplayName("Given an empty page, when getEmployees, then return empty without tenant lookup")
    void shouldReturnEmptyPageWithoutTenantLookup() {
        Page<Employee> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeListItemResponse> result = employeeService.getEmployees(
            new EmployeeFilter(null, null, null, null, null), PageRequest.of(0, 20));

        assertTrue(result.items().isEmpty());
        verify(tenantClient, never()).getTenantSummaries(any());
    }

    @Test
    @DisplayName("Given an existing employee, when deleteEmployee, then delete by id")
    void shouldDeleteEmployee() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(true);

        employeeService.deleteEmployee(id);

        verify(employeeRepository).deleteById(id);
    }

    @Test
    @DisplayName("Given an unknown employee, when deleteEmployee, then throw not found")
    void shouldThrowWhenDeleteNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> employeeService.deleteEmployee(id));
        verify(employeeRepository, never()).deleteById(any());
    }
}
