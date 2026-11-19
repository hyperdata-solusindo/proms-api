package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rincian jumlah task berdasarkan 4 kategori kolom")
public record BoardTaskCountsDto(
    @Schema(description = "Jumlah task di kolom To do", example = "3")
    long todo,
    @Schema(description = "Jumlah task di kolom Doing/In progress", example = "0")
    long doing,
    @Schema(description = "Jumlah task di kolom Blocked", example = "0")
    long blocked,
    @Schema(description = "Jumlah task di kolom Done", example = "0")
    long done) {
}
