package com.hyperdata.proms.module.pm.standup.dto;

public record StandupSummaryResponse(
    int submitted,
    int late,
    int missing,
    int exempt) {
}
