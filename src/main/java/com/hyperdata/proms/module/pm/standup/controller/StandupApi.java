package com.hyperdata.proms.module.pm.standup.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.pm.standup.dto.CreateStandupRequest;
import com.hyperdata.proms.module.pm.standup.dto.StandupDetailResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupListResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupTodayResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupUpdateResponse;
import com.hyperdata.proms.module.pm.standup.dto.UpdateStandupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Standup", description = "Manajemen laporan standup harian pengguna")
@RequestMapping("/api/v1")
public interface StandupApi {

    @Operation(
        summary = "Daftar Standup",
        description = "Mengambil rekap dan status standup seluruh anggota tenant aktif untuk hari ini.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daftar standup berhasil diambil"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/standups")
    RestResponse<StandupListResponse> getStandups();

    @Operation(
        summary = "Status Standup Hari Ini",
        description = "Mengambil status kewajiban standup hari ini beserta laporan yang sudah dikirim (jika ada).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status standup berhasil diambil"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/me/standup/today")
    RestResponse<StandupTodayResponse> getToday();

    @Operation(
        summary = "Riwayat Standup Saya",
        description = "Mengambil riwayat laporan standup milik pengguna pada rentang tanggal tertentu.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Riwayat standup berhasil diambil"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(schema = @Schema(implementation = RestResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Validasi gagal",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/me/standups")
    RestResponse<List<StandupDetailResponse>> getMyStandups(
        @RequestParam("from")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @RequestParam("to")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to);

    @Operation(
        summary = "Kirim Standup",
        description = "Mengirim laporan standup untuk hari ini. Satu pengguna hanya dapat mengirim satu laporan per hari.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Standup berhasil dikirim"),
        @ApiResponse(
            responseCode = "409",
            description = "Standup hari ini sudah dikirim",
            content = @Content(schema = @Schema(implementation = RestResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Validasi gagal atau standup tidak aktif untuk tenant",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })

    @Parameter(
        name = "Idempotency-key",
        in = ParameterIn.HEADER,
        description = "UUID unik per percobaan kirim (8-128 karakter) agar request yang diulang tidak membuat data ganda (P-07)",
        required = true,
        schema = @Schema(type = "string", minLength = 0, maxLength = 128),
        example = "3f2b8c1e-5a7d-4e9a-9b1c-2d4f6a8c0e12")
    @PostMapping("/me/standups")
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<StandupDetailResponse> createStandup(
        @RequestBody
        CreateStandupRequest request);

    @Operation(
        summary = "Perbarui Standup",
        description = "Memperbarui sebagian isi laporan standup milik pengguna. Wajib menyertakan header If-Match "
            + "berisi nilai version terakhir.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Standup berhasil diperbarui"),
        @ApiResponse(
            responseCode = "404",
            description = "Standup tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Versi data sudah berubah",
            content = @Content(schema = @Schema(implementation = RestResponse.class))),
        @ApiResponse(
            responseCode = "428",
            description = "Header If-Match tidak disertakan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @PatchMapping("/me/standups/{standupId}")
    RestResponse<StandupUpdateResponse> updateStandup(
        @Parameter(description = "UUID Standup", required = true)
        @PathVariable("standupId")
        UUID standupId,
        @Parameter(description = "Nilai version terakhir dari respons standup", required = true)
        @RequestHeader(value = "If-Match", required = false)
        String ifMatch,
        @RequestBody
        UpdateStandupRequest request);

}
