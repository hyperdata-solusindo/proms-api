package com.hyperdata.proms.module.work.board.dto;

import com.hyperdata.proms.common.docs.annotation.VersionEpochSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ringkasan board untuk tampilan list")
public record BoardSummaryResponse(
    UUID id,
    String key,
    String name,
    @Schema(description = "Grup divisi")
    BoardGroupDto group,
    String status,
    String health,
    String healthReason,
    Integer progressPct,
    LocalDate dueDate,
    PmSummaryResponse pm,
    Integer memberCount,
    String myRole,
    Instant archivedAt,
    @VersionEpochSchema
    Instant version) {

    public record PmSummaryResponse(UUID id, String fullName) {
    }
}
