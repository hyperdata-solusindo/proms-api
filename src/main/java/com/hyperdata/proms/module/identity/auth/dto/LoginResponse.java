package com.hyperdata.proms.module.identity.auth.dto;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload balasan sukses login pengguna")
public record LoginResponse(
    @Schema(description = "Token akses autentikasi sesi", example = "12|Xk9TokenExampleAbc123")
    String token,
    @Schema(description = "Tipe token otorisasi", example = "Bearer")
    String tokenType,
    @Schema(description = "Informasi ringkas pengguna yang login")
    AuthUserDto user,
    @Schema(description = "Informasi ringkas tenant aktif pengguna")
    TenantSummaryDto tenant,
    @Schema(description = "Peran pengguna dalam workspace tenant", example = "member")
    String workspaceRole) {
}
