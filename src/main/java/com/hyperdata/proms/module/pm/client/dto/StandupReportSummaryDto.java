package com.hyperdata.proms.module.pm.client.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StandupReportSummaryDto(
    UUID id,
    UUID userId,
    LocalDate reporDate,
    Instant submittedAt,
    boolean isLate) {

}
