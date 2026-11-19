package com.hyperdata.proms.module.pm.client.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskCreatedDto(
    UUID id,
    String key,
    String title,
    UUID columnId,
    UUID workstreamId,
    Instant version) {
}
