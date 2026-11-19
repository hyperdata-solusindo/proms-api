package com.hyperdata.proms.module.pm.standup.dto;

public record StandupEntryResponse(
    StandupUserResponse user,
    String status,
    String reason,
    boolean hasBlockers,
    StandupDetailResponse report) {
}
