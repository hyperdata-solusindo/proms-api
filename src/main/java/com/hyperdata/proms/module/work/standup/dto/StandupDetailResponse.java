package com.hyperdata.proms.module.work.standup.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StandupDetailResponse(
    UUID id,
    LocalDate reportDate,
    String plan,
    String done,
    String blockers,
    Instant submittedAt,
    boolean isLate,
    String version) {

}
