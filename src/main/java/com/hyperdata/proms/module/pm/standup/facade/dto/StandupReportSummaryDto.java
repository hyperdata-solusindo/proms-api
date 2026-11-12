package com.hyperdata.proms.module.pm.standup.facade.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StandupReportSummaryDto(
    UUID id,
    UUID userId,
    LocalDate reportDate,
    Instant submittedAt,
    boolean isLate) {

}
