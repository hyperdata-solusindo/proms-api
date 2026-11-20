package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Avatar ringkas anggota board")
public record BoardMemberAvatarDto(
    @Schema(description = "UUID Pengguna")
    UUID id,
    @Schema(description = "Inisial nama", example = "YA")
    String initials,
    @Schema(description = "URL foto avatar")
    String avatarUrl) {
}
