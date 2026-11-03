package com.hyperdata.proms.module.platform.tenant.facade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Ringkasan data tenant aktif pengguna")
public record TenantSummaryDto(
    @Schema(description = "ID unik tenant", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    UUID id,
    @Schema(description = "Slug pengenal unik URL tenant", example = "hyperdata-solusindo")
    String slug,
    @Schema(description = "Nama instansi atau perusahaan tenant", example = "PT Hyperdata Solusindo")
    String name) {
}
