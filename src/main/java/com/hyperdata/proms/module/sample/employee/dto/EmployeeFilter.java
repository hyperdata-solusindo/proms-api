package com.hyperdata.proms.module.sample.employee.dto;

import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Filter pencarian daftar karyawan")
public record EmployeeFilter(
    @Schema(description = "Kata kunci nama lengkap karyawan", example = "Budi")
    String name,

    @Schema(description = "Nomor induk karyawan", example = "EMP-0001")
    String employeeNumber,

    @Schema(description = "Nama departemen karyawan", example = "Engineering")
    String department,

    @Schema(description = "ID tenant pemilik karyawan", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    UUID tenantId,

    @Schema(description = "Status kepegawaian", example = "ACTIVE")
    EmployeeStatus status) {
}
