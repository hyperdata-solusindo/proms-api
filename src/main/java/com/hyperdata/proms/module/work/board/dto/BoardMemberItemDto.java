package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Item rincian data anggota board")
public record BoardMemberItemDto(
    @Schema(description = "UUID Board Member")
    UUID id,
    @Schema(description = "Informasi akun pengguna")
    BoardUserDto user,
    @Schema(description = "Role anggota (admin, pm, member, viewer, guest)", example = "member")
    String role,
    @Schema(description = "Status pemilik board", example = "false")
    boolean isOwner,
    @Schema(description = "Daftar workstream yang ditugaskan")
    List<UUID> workstreamIds,
    @Schema(description = "ETag versi optimistik concurrency", example = "\"12345\"")
    String version) {
}
