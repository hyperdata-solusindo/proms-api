package com.hyperdata.proms.module.sample.employee.service;

import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.security.TenantScoped;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.common.validation.YaviValidator;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeCacheNames;
import com.hyperdata.proms.module.sample.employee.dto.CreateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeListItemResponse;
import com.hyperdata.proms.module.sample.employee.dto.UpdateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapper;
import com.hyperdata.proms.module.sample.employee.repository.EmployeeRepository;
import com.hyperdata.proms.module.sample.employee.repository.spec.EmployeeSpecs;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@TenantScoped
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final TenantClient tenantClient;

    @Transactional
    public EmployeeDetailResponse createEmployee(CreateEmployeeRequest request) {
        YaviValidator.validateAndThrow(CreateEmployeeRequest.validator, request);

        TenantSummaryDto tenant = requireTenant(request.tenantId());

        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new DuplicateResourceException(
                Messages.get("employee.error.duplicate_number", request.employeeNumber()));
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                Messages.get("employee.error.duplicate_email", request.email()));
        }

        Employee employee = employeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDetailResponse(saved, tenant);
    }

    @Cacheable(cacheNames = EmployeeCacheNames.EMPLOYEE_DETAILS, key = "#id")
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        TenantSummaryDto tenant = tenantClient.getTenantSummary(employee.getTenantId()).orElse(null);
        return employeeMapper.toDetailResponse(employee, tenant);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeListItemResponse> getEmployees(EmployeeFilter filter, Pageable pageable) {
        Page<Employee> employeePage = employeeRepository.findAll(EmployeeSpecs.of(filter), pageable);
        List<Employee> employees = employeePage.getContent();
        if (employees.isEmpty()) {
            return PageResponse.from(employeePage, List.of());
        }

        Set<UUID> tenantIds = Lookups.extractIds(employees, Employee::getTenantId);
        List<TenantSummaryDto> tenants = tenantClient.getTenantSummaries(tenantIds);
        Map<UUID, TenantSummaryDto> tenantMap = Lookups.toMap(tenants, TenantSummaryDto::id);

        List<EmployeeListItemResponse> items = Lookups.map(
            employees, employee -> employeeMapper.toListItemResponse(employee, tenantMap.get(employee.getTenantId())));

        return PageResponse.from(employeePage, items);
    }

    @CacheEvict(cacheNames = EmployeeCacheNames.EMPLOYEE_DETAILS, key = "#id")
    @Transactional
    public EmployeeDetailResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {
        YaviValidator.validateAndThrow(UpdateEmployeeRequest.validator, request);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        TenantSummaryDto tenant = requireTenant(request.tenantId());

        if (employeeRepository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), id)) {
            throw new DuplicateResourceException(
                Messages.get("employee.error.duplicate_number", request.employeeNumber()));
        }
        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException(
                Messages.get("employee.error.duplicate_email", request.email()));
        }

        employee.setTenantId(request.tenantId());
        employee.setEmployeeNumber(request.employeeNumber());
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setDepartment(request.department());
        employee.setPosition(request.position());
        employee.setStatus(request.status());

        Employee updated = employeeRepository.save(employee);
        return employeeMapper.toDetailResponse(updated, tenant);
    }

    @CacheEvict(cacheNames = EmployeeCacheNames.EMPLOYEE_DETAILS, key = "#id")
    @Transactional
    public void deleteEmployee(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        employeeRepository.deleteById(id);
    }

    private TenantSummaryDto requireTenant(UUID tenantId) {
        return tenantClient.getTenantSummary(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
    }
}
