package com.hyperdata.proms.module.sample.employee.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmployeeRequestValidationTest {

    private static CreateEmployeeRequest validCreate() {
        return new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("Given a valid create request with null phone, when validated, then it is valid")
    void shouldAcceptValidCreateRequestWithNullPhone() {
        assertTrue(CreateEmployeeRequest.validator.validate(validCreate()).isValid());
    }

    @Test
    @DisplayName("Given a create request without tenant, when validated, then it is invalid")
    void shouldRejectMissingTenant() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            null, "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE);
        assertFalse(CreateEmployeeRequest.validator.validate(request).isValid());
    }

    @Test
    @DisplayName("Given a create request with invalid email, when validated, then it is invalid")
    void shouldRejectInvalidEmail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "not-an-email",
            null, "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE);
        assertFalse(CreateEmployeeRequest.validator.validate(request).isValid());
    }

    @Test
    @DisplayName("Given a create request without join date, when validated, then it is invalid")
    void shouldRejectMissingJoinDate() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", null, EmployeeStatus.ACTIVE);
        assertFalse(CreateEmployeeRequest.validator.validate(request).isValid());
    }

    @Test
    @DisplayName("Given an update request without status, when validated, then it is invalid")
    void shouldRejectUpdateWithoutStatus() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", null);
        assertFalse(UpdateEmployeeRequest.validator.validate(request).isValid());
    }
}
