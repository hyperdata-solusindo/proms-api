package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Metadata halaman daftar/kartu board")
public record BoardPageMetaDto(
    @Schema(description = "Total board yang ditemukan", example = "2")
    long totalBoards,
    @Schema(description = "Kursor halaman berikutnya")
    String nextCursor,
    @Schema(description = "Ringkasan metrik status board")
    BoardSummaryMetaDto summary,
    @Schema(description = "Daftar chip filter grup beserta jumlah board")
    List<BoardGroupFilterChipDto> groups) {

    @Schema(description = "Ringkasan metrik kesehatan board")
    public record BoardSummaryMetaDto(
        @Schema(description = "Total board aktif", example = "2")
        long activeBoards,
        @Schema(description = "Total board berstatus on track", example = "1")
        long onTrack,
        @Schema(description = "Total board berstatus at risk", example = "0")
        long atRisk) {
    }

    @Schema(description = "Data filter chip grup")
    public record BoardGroupFilterChipDto(
        @Schema(description = "ID grup atau 'all'", example = "all")
        String id,
        @Schema(description = "Nama grup", example = "All")
        String name,
        @Schema(description = "Jumlah board dalam grup", example = "1")
        long count) {
    }
}
