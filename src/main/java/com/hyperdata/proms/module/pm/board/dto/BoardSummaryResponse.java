package com.hyperdata.proms.module.pm.board.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BoardSummaryResponse(
    UUID id,
    String key,
    String name,
    String status,
    String health,
    String healthReason,
    Integer progressPct,
    LocalDate dueDate,
    PmSummaryResponse pm,
    Integer memberCount,
    String myRole,
    Instant archivedAt,
    Instant version) {

    public record PmSummaryResponse(UUID id, String fullName) {
    }
}
