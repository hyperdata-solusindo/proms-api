package com.hyperdata.proms.module.work.client.dto;

import java.util.UUID;

public record BoardPermissionDto(
    UUID boardId,
    UUID userId,
    String role,
    boolean isOwner,
    String boardStatus) {
}
