package com.hyperdata.proms.module.sample.employee.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Payload permintaan pembaruan data karyawan")
public record UpdateEmployeeRequest(
    @Schema(
        description = "ID tenant pemilik karyawan",
        example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
        requiredMode = Schema.RequiredMode.REQUIRED)
    UUID tenantId,

    @Schema(
        description = "Nomor induk karyawan",
        example = "EMP-0001",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String employeeNumber,

    @Schema(
        description = "Nama lengkap karyawan",
        example = "Budi Santoso",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String fullName,

    @Schema(
        description = "Alamat email karyawan",
        example = "budi.santoso@hyperdata.co.id",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String email,

    @Schema(
        description = "Nomor telepon karyawan",
        example = "+6281234567890")
    String phone,

    @Schema(
        description = "Departemen karyawan",
        example = "Engineering",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String department,

    @Schema(
        description = "Jabatan karyawan",
        example = "Backend Engineer",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String position,

    @Schema(
        description = "Status kepegawaian",
        example = "ACTIVE",
        requiredMode = Schema.RequiredMode.REQUIRED)
    EmployeeStatus status) {

    public static final Validator<UpdateEmployeeRequest> validator = ValidatorBuilder.<UpdateEmployeeRequest>of()
        .constraintOnObject(UpdateEmployeeRequest::tenantId, "tenantId",
            c -> c.notNull().message("Tenant is required"))
        .constraint(UpdateEmployeeRequest::employeeNumber, "employeeNumber",
            c -> c.notBlank()
                .message("Employee number is required")
                .lessThanOrEqual(32)
                .message("Employee number max 32 chars"))
        .constraint(UpdateEmployeeRequest::fullName, "fullName",
            c -> c.notBlank()
                .message("Full name is required")
                .lessThanOrEqual(128)
                .message("Full name max 128 chars"))
        .constraint(UpdateEmployeeRequest::email, "email",
            c -> c.notBlank()
                .message("Email is required")
                .email()
                .message("Email format is invalid")
                .lessThanOrEqual(128)
                .message("Email max 128 chars"))
        .constraint(UpdateEmployeeRequest::phone, "phone",
            c -> c.lessThanOrEqual(32).message("Phone max 32 chars"))
        .constraint(UpdateEmployeeRequest::department, "department",
            c -> c.notBlank()
                .message("Department is required")
                .lessThanOrEqual(64)
                .message("Department max 64 chars"))
        .constraint(UpdateEmployeeRequest::position, "position",
            c -> c.notBlank()
                .message("Position is required")
                .lessThanOrEqual(64)
                .message("Position max 64 chars"))
        .constraint(UpdateEmployeeRequest::status, "status",
            c -> c.notNull().message("Status is required"))
        .build();
}
