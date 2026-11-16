package com.hyperdata.proms.module.pm.client.dto;

import java.time.LocalTime;
import java.util.UUID;

public record StandupPolicyDto(
    UUID tenantId,
    boolean enabled,
    boolean reminderEnabled,
    LocalTime reminderTime,
    LocalTime deadlinTime,
    boolean blockingBanner) {
}
