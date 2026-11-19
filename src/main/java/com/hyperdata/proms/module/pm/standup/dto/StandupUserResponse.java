package com.hyperdata.proms.module.pm.standup.dto;

import java.util.UUID;

public record StandupUserResponse(
    UUID id,
    String fullName,
    String avatarUrl) {
}
