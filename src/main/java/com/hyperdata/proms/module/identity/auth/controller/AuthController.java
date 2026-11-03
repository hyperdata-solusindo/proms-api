package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Manajemen autentikasi, token sesi, dan keamanan akun")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Login Pengguna",
        description = "Memverifikasi email dan kata sandi, lalu menerbitkan personal access token beserta ringkasan tenant aktif.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login berhasil, token diterbitkan"),
        @ApiResponse(
            responseCode = "400",
            description = "Format request tidak valid / validasi YAVI gagal",
            content = @Content(
                schema = @Schema(
                    implementation = com.hyperdata.proms.common.response.ApiResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Email atau kata sandi salah",
            content = @Content(
                schema = @Schema(
                    implementation = com.hyperdata.proms.common.response.ApiResponse.class)))
    })
    @SecurityRequirements()
    @PostMapping(
        value = "/auth/login",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public com.hyperdata.proms.common.response.ApiResponse<LoginResponse> login(@RequestBody
    LoginRequest request) {
        LoginResponse response = authService.login(request);
        return com.hyperdata.proms.common.response.ApiResponse.ok(response, "Berhasil login");
    }

    @Operation(
        summary = "Logout Pengguna",
        description = "Mencabut personal access token sesi aktif saat ini sehingga tidak dapat digunakan kembali.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil logout, sesi dinonaktifkan"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(
                schema = @Schema(
                    implementation = com.hyperdata.proms.common.response.ApiResponse.class)))
    })
    @PostMapping(value = "/auth/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public com.hyperdata.proms.common.response.ApiResponse<Void> logout() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        authService.logout(currentUser.tokenId());
        return com.hyperdata.proms.common.response.ApiResponse.ok(null, "Berhasil logout");
    }
}
