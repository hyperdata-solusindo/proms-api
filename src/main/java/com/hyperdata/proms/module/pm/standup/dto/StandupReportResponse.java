package com.hyperdata.proms.module.pm.standup.dto;

import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StandupReportResponse(
    UUID id,
    LocalDate reportDate,
    String plan,
    String done,
    String blockers,
    Instant submittedAt,
    boolean isLate,
    String version) {

    public static StandupReportResponse from(StandupReports report) {
        return new StandupReportResponse(
            report.getId(),
            report.getReportDate(),
            report.getPlan(),
            report.getDone(),
            report.getBlockers(),
            report.getSubmittedAt(),
            report.isLate(),
            null);
    }
}
