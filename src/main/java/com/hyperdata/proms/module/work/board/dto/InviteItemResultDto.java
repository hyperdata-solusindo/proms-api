package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Hasil pemrosesan undangan per email")
public record InviteItemResultDto(
    @Schema(description = "Email tujuan", example = "budi@contoh.id")
    String email,
    @Schema(description = "Hasil proses (added, invited, already_member, invalid)", example = "added")
    String result,
    @Schema(description = "UUID BoardMember jika user langsung ditambahkan")
    UUID memberId,
    @Schema(description = "UUID BoardInvitation jika undangan baru dibuat")
    UUID invitationId) {
}
