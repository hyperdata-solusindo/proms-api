package com.hyperdata.proms.module.pm.standup.facade.dto;

import java.time.LocalTime;
import java.util.UUID;

public record StandupPolicyDto(
    UUID tenantId,
    boolean enabled,
    boolean reminderEnabled,
    LocalTime reminderTime,
    LocalTime deadlineTime,
    boolean blockingBanner) {

}
