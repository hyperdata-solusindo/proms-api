package com.hyperdata.proms.module.pm.client.dto;

import java.util.UUID;

public record BoardPermissionDto(
    UUID boardId,
    UUID userId,
    String role,
    boolean isOwner,
    String boardStatus) {
}
