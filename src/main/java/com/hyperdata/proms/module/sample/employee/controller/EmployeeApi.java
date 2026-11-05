package com.hyperdata.proms.module.sample.employee.controller;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.sample.employee.dto.CreateEmployeeRequest;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeListItemResponse;
import com.hyperdata.proms.module.sample.employee.dto.UpdateEmployeeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Employees", description = "Manajemen data master karyawan")
@RequestMapping("/api/v1/employees")
public interface EmployeeApi {

    @Operation(
        summary = "Tambah Karyawan Baru",
        description = "Membuat data karyawan baru di dalam sistem.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Karyawan berhasil dibuat")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<EmployeeDetailResponse> createEmployee(@RequestBody
    CreateEmployeeRequest request);

    @Operation(
        summary = "Ambil Detail Karyawan",
        description = "Mengambil data lengkap karyawan berdasarkan UUID spesifik.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data karyawan ditemukan"),
        @ApiResponse(
            responseCode = "404",
            description = "Karyawan tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/{id}")
    RestResponse<EmployeeDetailResponse> getEmployeeById(
        @Parameter(description = "ID unik karyawan", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable
        UUID id);

    @Operation(
        summary = "Daftar Karyawan",
        description = "Mengambil daftar karyawan dengan pagination dan filter.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daftar karyawan berhasil diambil")
    })
    @GetMapping
    PageResponse<EmployeeListItemResponse> getEmployees(
        EmployeeFilter filter,
        @PageableDefault(size = 20)
        Pageable pageable);

    @Operation(
        summary = "Perbarui Karyawan",
        description = "Memperbarui data karyawan berdasarkan ID spesifik.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Karyawan berhasil diperbarui"),
        @ApiResponse(
            responseCode = "404",
            description = "Karyawan tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @PutMapping("/{id}")
    RestResponse<EmployeeDetailResponse> updateEmployee(
        @Parameter(description = "ID unik karyawan", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable
        UUID id,
        @RequestBody
        UpdateEmployeeRequest request);

    @Operation(
        summary = "Hapus Karyawan",
        description = "Menghapus data karyawan dari sistem berdasarkan ID spesifik.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Karyawan berhasil dihapus"),
        @ApiResponse(
            responseCode = "404",
            description = "Karyawan tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @DeleteMapping("/{id}")
    RestResponse<Void> deleteEmployee(
        @Parameter(description = "ID unik karyawan", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable
        UUID id);
}
