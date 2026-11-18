package com.hyperdata.proms.module.pm.focus.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FocusSessionResponse(
    UUID id,
    Instant startedAt,
    Instant endedAt,
    int durationSec,
    int durationMin,
    LocalDate date,
    TaskRef task) {

    public record TaskRef(UUID id, String key, String title) {
    }
}
