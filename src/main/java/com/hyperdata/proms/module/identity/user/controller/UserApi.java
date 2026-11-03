package com.hyperdata.proms.module.identity.user.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "User Profile", description = "Manajemen data profil dan informasi sesi pengguna")
public interface UserApi {

    @Operation(
        summary = "Ambil Profil Pengguna",
        description = "Mengambil data profil dan akun pengguna yang sedang terautentikasi.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data profil berhasil diambil"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/me")
    RestResponse<MeResponse> getMe();
}
