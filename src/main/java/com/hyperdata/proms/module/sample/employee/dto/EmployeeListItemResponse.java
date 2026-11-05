package com.hyperdata.proms.module.sample.employee.dto;

import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Payload ringkas data karyawan untuk daftar")
public record EmployeeListItemResponse(
    @Schema(description = "ID unik karyawan", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    UUID id,

    @Schema(description = "Nomor induk karyawan", example = "EMP-0001")
    String employeeNumber,

    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    String fullName,

    @Schema(description = "Departemen karyawan", example = "Engineering")
    String department,

    @Schema(description = "Jabatan karyawan", example = "Backend Engineer")
    String position,

    @Schema(description = "Status kepegawaian", example = "ACTIVE")
    EmployeeStatus status,

    @Schema(description = "ID tenant pemilik karyawan", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    UUID tenantId,

    @Schema(description = "Nama tenant pemilik karyawan", example = "PT Hyperdata Solusindo")
    String tenantName) {
}
