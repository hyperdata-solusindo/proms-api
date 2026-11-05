package com.hyperdata.proms.module.sample.employee.repository.spec;

import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecs {

    private EmployeeSpecs() {
    }

    public static Specification<Employee> of(EmployeeFilter filter) {
        Specification<Employee> spec = (root, query, cb) -> cb.conjunction();
        if (filter == null) {
            return spec;
        }
        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and(fullNameContains(filter.name().trim()));
        }
        if (filter.employeeNumber() != null && !filter.employeeNumber().isBlank()) {
            spec = spec.and(employeeNumberEquals(filter.employeeNumber().trim()));
        }
        if (filter.department() != null && !filter.department().isBlank()) {
            spec = spec.and(departmentContains(filter.department().trim()));
        }
        if (filter.tenantId() != null) {
            spec = spec.and(hasTenantId(filter.tenantId()));
        }
        if (filter.status() != null) {
            spec = spec.and(hasStatus(filter.status()));
        }
        return spec;
    }

    public static Specification<Employee> fullNameContains(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("fullName")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Employee> employeeNumberEquals(String employeeNumber) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("employeeNumber")), employeeNumber.toLowerCase());
    }

    public static Specification<Employee> departmentContains(String department) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("department")), "%" + department.toLowerCase() + "%");
    }

    public static Specification<Employee> hasTenantId(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Employee> hasStatus(EmployeeStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
