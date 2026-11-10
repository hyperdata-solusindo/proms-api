package com.hyperdata.proms.module.pm.board.facade.dto;

import java.util.UUID;

public record BoardPermissionDto(
    UUID boardId,
    UUID userId,
    String role,
    boolean isOwner,
    String boardStatus) {
}
