package com.hyperdata.proms.module.identity.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Data ringkas pengguna terautentikasi")
public record AuthUserDto(
    @Schema(description = "ID unik pengguna", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID id,
    @Schema(description = "Nama lengkap pengguna", example = "Budi Santoso")
    String fullName,
    @Schema(description = "Alamat email pengguna", example = "admin@proms.com", format = "email")
    String email,
    @Schema(description = "Kode bahasa preferensi pengguna", example = "id")
    String locale,
    @Schema(description = "Zona waktu pengguna", example = "Asia/Jakarta")
    String timezone) {
}
