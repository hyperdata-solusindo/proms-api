package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Data undangan tertunda")
public record PendingInvitationDto(
    @Schema(description = "UUID undangan")
    UUID id,
    @Schema(description = "Email yang diundang", example = "calon@contoh.id")
    String email,
    @Schema(description = "Role yang ditawarkan", example = "member")
    String role,
    @Schema(description = "Waktu kadaluarsa undangan")
    Instant expiresAt) {
}
