package com.hyperdata.proms.module.pm.focus.dto;

import java.time.LocalDate;

public record FocusSummaryResponse(
    long totalSec,
    long totalMin,
    int capacityMin,
    LocalDate weekStart,
    LocalDate weekEnd) {

}
