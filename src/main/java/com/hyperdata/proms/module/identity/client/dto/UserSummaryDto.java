package com.hyperdata.proms.module.identity.client.dto;

import java.util.UUID;

public record UserSummaryDto(
    UUID id,
    String name,
    String email,
    String avatarUrl) {
}
