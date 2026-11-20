package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Representasi lengkap satu kartu board pada Card View")
public record BoardCardResponse(
    @Schema(description = "UUID board")
    UUID id,
    @Schema(description = "Kode unik board", example = "NBI")
    String key,
    @Schema(description = "Nama board", example = "Nusa Bioscience IoT")
    String name,
    @Schema(description = "Grup divisi")
    BoardGroupDto group,
    @Schema(description = "Klien")
    BoardClientDto client,
    @Schema(description = "Status kesehatan board", example = "on_track")
    String health,
    @Schema(description = "Alasan status kesehatan", example = "Semua milestone selesai tepat waktu")
    String healthReason,
    @Schema(description = "Jumlah task aktif (belum selesai)", example = "1")
    long activeTaskCount,
    @Schema(description = "Rincian jumlah task per kategori kolom")
    BoardTaskCountsDto taskCounts,
    @Schema(description = "Jumlah task selesai", example = "0")
    long doneCount,
    @Schema(description = "Total seluruh task", example = "3")
    long totalCount,
    @Schema(description = "Persentase penyelesaian task (0-100)", example = "0")
    int progressPct,
    @Schema(description = "Tanggal jatuh tempo", example = "2026-12-18")
    LocalDate dueDate,
    @Schema(description = "Sisa hari menuju jatuh tempo", example = "56")
    Long daysLeft,
    @Schema(description = "Avatar 3 anggota teratas")
    List<BoardMemberAvatarDto> members,
    @Schema(description = "Total jumlah anggota board", example = "2")
    int memberCount,
    @Schema(description = "ETag versi board untuk optimasi concurrency", example = "\"1\"")
    String version) {
}
