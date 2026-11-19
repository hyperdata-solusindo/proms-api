package com.hyperdata.proms.module.pm.standup.dto;

import java.time.LocalDate;
import java.util.List;

public record StandupListResponse(
    LocalDate date,
    StandupSummaryResponse summary,
    List<StandupEntryResponse> data) {
}
