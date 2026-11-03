package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "Manajemen autentikasi, token sesi, dan keamanan akun")
public interface AuthApi {

    @Operation(
        summary = "Login Pengguna",
        description = "Memverifikasi email dan kata sandi, lalu menerbitkan personal access token beserta ringkasan tenant aktif.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login berhasil, token diterbitkan"),
        @ApiResponse(
            responseCode = "401",
            description = "Email atau kata sandi salah",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/auth/login")
    RestResponse<LoginResponse> login(@RequestBody
    LoginRequest request);

    @Operation(
        summary = "Logout Pengguna",
        description = "Mencabut personal access token sesi aktif saat ini sehingga tidak dapat digunakan kembali.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil logout, sesi dinonaktifkan"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @PostMapping("/auth/logout")
    RestResponse<Void> logout();
}
