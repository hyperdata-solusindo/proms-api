package com.hyperdata.proms.module.pm.client.dto;

import java.time.Instant;
import java.util.UUID;

public record FocusSessionSummaryDto(
    UUID ID,
    UUID userId,
    Instant startedAt,
    Instant endedAt,
    int durationSec) {

}
