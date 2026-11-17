package com.hyperdata.proms.module.pm.standup.dto;

import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import java.util.UUID;

public record StandupUpdateResponse(
    UUID id,
    String plan,
    String done,
    String blockers,
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
            report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : null);
    }
}
