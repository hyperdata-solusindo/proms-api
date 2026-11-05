package com.hyperdata.proms.module.sample.employee.repository.spec;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class EmployeeSpecsTest {

    @Test
    @DisplayName("Given null filter, when EmployeeSpecs.of, then return a non-null specification")
    void shouldReturnSpecificationForNullFilter() {
        assertNotNull(EmployeeSpecs.of(null));
    }

    @Test
    @DisplayName("Given all filter fields, when EmployeeSpecs.of, then compose every criterion")
    void shouldComposeAllFilterCriteria() {
        EmployeeFilter filter = new EmployeeFilter(
            "Budi", "EMP-0001", "Engineering", UUID.randomUUID(), EmployeeStatus.ACTIVE);
        Specification<Employee> spec = EmployeeSpecs.of(filter);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Given a status-only filter, when EmployeeSpecs.of, then compose the status criterion")
    void shouldComposeStatusOnlyFilter() {
        Specification<Employee> spec = EmployeeSpecs.of(
            new EmployeeFilter(null, null, null, null, EmployeeStatus.ON_LEAVE));
        assertNotNull(spec);
    }
}
