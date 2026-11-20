package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Respon daftar anggota board dan undangan tertunda")
public record BoardMemberListResponse(
    @Schema(description = "Daftar anggota aktif")
    List<BoardMemberItemDto> data,
    @Schema(description = "Daftar undangan yang belum diterima/kadaluarsa")
    List<PendingInvitationDto> pendingInvitations) {
}
