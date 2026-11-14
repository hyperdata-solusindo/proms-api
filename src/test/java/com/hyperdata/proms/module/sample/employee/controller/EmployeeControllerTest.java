package com.hyperdata.proms.module.sample.employee.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.GlobalExceptionHandler;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.sample.employee.dto.CreateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeListItemResponse;
import com.hyperdata.proms.module.sample.employee.dto.UpdateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import com.hyperdata.proms.module.sample.employee.service.EmployeeService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    private EmployeeDetailResponse detail(UUID id, String fullName) {
        return new EmployeeDetailResponse(
            id, "EMP-0001", fullName, "budi.santoso@hyperdata.co.id", "+6281234567890",
            "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE, UUID.randomUUID(), "PT Hyperdata Solusindo",
            "hyperdata-solusindo", Instant.now());
    }

    @Test
    @DisplayName("Given valid CreateEmployeeRequest, when POST /api/v1/employees, then return 201 Created")
    void shouldCreateEmployee() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Backend Engineer",
            LocalDate.parse("2024-01-15"), EmployeeStatus.ACTIVE);
        UUID id = UUID.randomUUID();
        EmployeeDetailResponse response = detail(id, "Budi Santoso");

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/employees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.id").value(id.toString()))
            .andExpect(jsonPath("$.data.employeeNumber").value("EMP-0001"))
            .andExpect(jsonPath("$.data.tenantName").value("PT Hyperdata Solusindo"));
    }

    @Test
    @DisplayName("Given existing employee ID, when GET /api/v1/employees/{id}, then return 200 OK")
    void shouldGetEmployeeById() throws Exception {
        UUID id = UUID.randomUUID();
        EmployeeDetailResponse response = detail(id, "Budi Santoso");

        when(employeeService.getEmployeeById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/employees/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.fullName").value("Budi Santoso"))
            .andExpect(jsonPath("$.data.tenantSlug").value("hyperdata-solusindo"));
    }

    @Test
    @DisplayName("Given non-existent employee ID, when GET /api/v1/employees/{id}, then return 404 Not Found")
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.getEmployeeById(id)).thenThrow(new ResourceNotFoundException("Employee", id));

        mockMvc.perform(get("/api/v1/employees/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Given pageable and name filter, when GET /api/v1/employees, then return PageResponse")
    void shouldGetEmployeesPage() throws Exception {
        UUID id = UUID.randomUUID();
        EmployeeListItemResponse item = new EmployeeListItemResponse(
            id, "EMP-0001", "Budi Santoso", "Engineering", "Backend Engineer",
            EmployeeStatus.ACTIVE, UUID.randomUUID(), "PT Hyperdata Solusindo");
        PageResponse<EmployeeListItemResponse> pageResponse = new PageResponse<>(
            List.of(item), 0, 20, 1, 1, true, true);

        when(employeeService.getEmployees(any(EmployeeFilter.class), any(Pageable.class)))
            .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/employees")
            .param("name", "Budi")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.items[0].tenantName").value("PT Hyperdata Solusindo"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Given valid UpdateEmployeeRequest, when PUT /api/v1/employees/{id}, then return 200 OK")
    void shouldUpdateEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso Updated", "budi.santoso@hyperdata.co.id",
            "+6281234567890", "Engineering", "Senior Backend Engineer", EmployeeStatus.ACTIVE);
        EmployeeDetailResponse response = detail(id, "Budi Santoso Updated");

        when(employeeService.updateEmployee(eq(id), any(UpdateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/employees/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.fullName").value("Budi Santoso Updated"));
    }

    @Test
    @DisplayName("Given existing employee ID, when DELETE /api/v1/employees/{id}, then return 200 OK")
    void shouldDeleteEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(employeeService).deleteEmployee(id);

        mockMvc.perform(delete("/api/v1/employees/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Karyawan berhasil dihapus"));

        mockMvc.perform(delete("/api/v1/employees/{id}", id).header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Employee deleted successfully"));
    }
}
