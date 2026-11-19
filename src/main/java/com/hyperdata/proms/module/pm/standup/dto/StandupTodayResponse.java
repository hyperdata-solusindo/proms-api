package com.hyperdata.proms.module.pm.standup.dto;

import java.time.LocalDate;

public record StandupTodayResponse(
    LocalDate reportDate,
    boolean required,
    String reason,
    StandupDetailResponse report,
    String deadlineTime,
    boolean blockingBanner) {

}
