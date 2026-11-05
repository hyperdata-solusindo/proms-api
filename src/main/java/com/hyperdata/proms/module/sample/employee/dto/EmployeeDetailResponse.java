package com.hyperdata.proms.module.sample.employee.dto;

import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Payload detail data karyawan")
public record EmployeeDetailResponse(
    @Schema(description = "ID unik karyawan", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    UUID id,

    @Schema(description = "Nomor induk karyawan", example = "EMP-0001")
    String employeeNumber,

    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    String fullName,

    @Schema(description = "Alamat email karyawan", example = "budi.santoso@hyperdata.co.id")
    String email,

    @Schema(description = "Nomor telepon karyawan", example = "+6281234567890")
    String phone,

    @Schema(description = "Departemen karyawan", example = "Engineering")
    String department,

    @Schema(description = "Jabatan karyawan", example = "Backend Engineer")
    String position,

    @Schema(description = "Tanggal bergabung karyawan", example = "2024-01-15")
    LocalDate joinDate,

    @Schema(description = "Status kepegawaian", example = "ACTIVE")
    EmployeeStatus status,

    @Schema(description = "ID tenant pemilik karyawan", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    UUID tenantId,

    @Schema(description = "Nama tenant pemilik karyawan", example = "PT Hyperdata Solusindo")
    String tenantName,

    @Schema(description = "Slug tenant pemilik karyawan", example = "hyperdata-solusindo")
    String tenantSlug,

    @Schema(description = "Waktu data karyawan dibuat", example = "2026-11-05T08:30:00Z")
    Instant createdAt) {
}
