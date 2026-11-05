package com.hyperdata.proms.module.sample.employee.repository;

import com.hyperdata.proms.module.sample.employee.entity.Employee;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByEmail(String email);

    boolean existsByEmployeeNumberAndIdNot(String employeeNumber, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
