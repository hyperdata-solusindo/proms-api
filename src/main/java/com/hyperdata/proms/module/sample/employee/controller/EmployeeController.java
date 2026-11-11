package com.hyperdata.proms.module.sample.employee.controller;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.sample.employee.dto.CreateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeListItemResponse;
import com.hyperdata.proms.module.sample.employee.dto.UpdateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.service.EmployeeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmployeeController implements EmployeeApi {

    private final EmployeeService employeeService;

    @Override
    public RestResponse<EmployeeDetailResponse> createEmployee(CreateEmployeeRequest request) {
        EmployeeDetailResponse response = employeeService.createEmployee(request);
        return RestResponse.created(response, Messages.get("employee.created"));
    }

    @Override
    public RestResponse<EmployeeDetailResponse> getEmployeeById(UUID id) {
        EmployeeDetailResponse response = employeeService.getEmployeeById(id);
        return RestResponse.ok(response, Messages.get("employee.retrieved"));
    }

    @Override
    public RestResponse<PageResponse<EmployeeListItemResponse>> getEmployees(
        EmployeeFilter filter,
        Pageable pageable) {
        return RestResponse.ok(employeeService.getEmployees(filter, pageable), Messages.get("employee.list.retrieved"));
    }

    @Override
    public RestResponse<EmployeeDetailResponse> updateEmployee(
        UUID id,
        UpdateEmployeeRequest request) {
        EmployeeDetailResponse response = employeeService.updateEmployee(id, request);
        return RestResponse.ok(response, Messages.get("employee.updated"));
    }

    @Override
    public RestResponse<Void> deleteEmployee(UUID id) {
        employeeService.deleteEmployee(id);
        return RestResponse.ok(null, Messages.get("employee.deleted"));
    }
}
