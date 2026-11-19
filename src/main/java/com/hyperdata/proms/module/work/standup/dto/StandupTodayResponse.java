package com.hyperdata.proms.module.work.standup.dto;

import java.time.LocalDate;

public record StandupTodayResponse(
    LocalDate reportDate,
    boolean required,
    String reason,
    StandupDetailResponse report,
    String deadlineTime,
    boolean blockingBanner) {

}
