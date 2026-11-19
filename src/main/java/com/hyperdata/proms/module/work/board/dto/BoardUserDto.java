package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Data ringkas profil pengguna board")
public record BoardUserDto(
    @Schema(description = "UUID pengguna")
    UUID id,
    @Schema(description = "Nama lengkap pengguna", example = "Budi Santoso")
    String fullName,
    @Schema(description = "URL foto profil")
    String avatarUrl,
    @Schema(description = "Alamat email", example = "budi@contoh.id")
    String email) {
}
