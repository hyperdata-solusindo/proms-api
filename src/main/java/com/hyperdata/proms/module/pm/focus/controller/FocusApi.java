package com.hyperdata.proms.module.pm.focus.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.pm.focus.dto.CreateFocusSessionRequest;
import com.hyperdata.proms.module.pm.focus.dto.FocusSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Focus", description = "Pencatatan sesi fokus (timer) pengguna")
@RequestMapping("/api/v1")
public interface FocusApi {

    @Operation(
        summary = "Daftar Sesi Fokus",
        description = "Mengambil sesi fokus milik pengguna pada rentang tanggal tertentu. Ringkasan mingguan "
            + "(totalSec, totalMin, capacityMin, weekStart, weekEnd) dikirim pada `meta.summary`. "
            + "Jika `from` kosong dipakai hari ini; jika `to` kosong dipakai akhir minggu dari `from`.")
    @ApiResponse(responseCode = "200", description = "Daftar sesi fokus berhasil diambil")
    @ApiResponse(
        responseCode = "400",
        description = "Rentang tanggal tidak valid (to lebih awal dari from)",
        content = @Content(schema = @Schema(implementation = RestResponse.class)))
    @GetMapping("/focus-sessions")
    RestResponse<List<FocusSessionResponse>> listSessions(
        @Parameter(description = "Tanggal awal (yyyy-MM-dd). Default: hari ini", example = "2026-11-16")
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @Parameter(
            description = "Tanggal akhir inklusif (yyyy-MM-dd). Default: akhir minggu dari `from`",
            example = "2026-11-22")
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to);

    @Operation(
        summary = "Simpan Sesi Fokus",
        description = "Menyimpan satu sesi fokus saat pengguna menekan \"Stop & simpan\". Durasi "
            + "(endedAt - startedAt) wajib 1 menit sampai 16 jam, jika tidak maka 422 BR-FC-01 pada field `endedAt`.",
        parameters = @Parameter(
            name = "Idempotency-Key",
            in = ParameterIn.HEADER,
            required = false,
            description = "UUID unik per percobaan simpan agar retry tidak menggandakan sesi (P-07)",
            schema = @Schema(type = "string", format = "uuid")))
    @ApiResponse(responseCode = "201", description = "Sesi fokus berhasil disimpan")
    @ApiResponse(
        responseCode = "422",
        description = "Validasi gagal atau durasi di luar 1 menit - 16 jam (BR-FC-01)",
        content = @Content(schema = @Schema(implementation = RestResponse.class)))
    @PostMapping("/focus-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<FocusSessionResponse> createSession(
        @RequestBody
        CreateFocusSessionRequest request);
}
