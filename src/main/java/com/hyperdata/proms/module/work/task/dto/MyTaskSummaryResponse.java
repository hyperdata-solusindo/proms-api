package com.hyperdata.proms.module.work.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jumlah task saya per segmen jatuh tempo (zona waktu tenant")
public record MyTaskSummaryResponse(
    @Schema(description = "Jatuh Tempo hari ini", example = "3")
    long today,
    @Schema(description = "Terlambat", example = "1")
    long overdue,
    @Schema(description = "Jatuh tempo minggu ini", example = "7")
    long thisweek,
    @Schema(description = "Semua task aktif saya", example = "18")
    long all) {

}
