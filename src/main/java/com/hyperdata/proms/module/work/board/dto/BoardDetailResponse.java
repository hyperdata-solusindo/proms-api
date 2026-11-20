package com.hyperdata.proms.module.work.board.dto;

import com.hyperdata.proms.common.docs.annotation.VersionEpochSchema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoardDetailResponse(
    UUID id,
    String key,
    String name,
    String status,
    List<WorkstreamResponse> workstreams,
    List<BoardColumnResponse> columns,
    List<BoardMemberResponse> members,
    BoardPermissionResponse permissions,
    @VersionEpochSchema
    Instant version) {

    public record WorkstreamResponse(UUID id, String name, String color, Integer position) {
    }

    public record BoardColumnResponse(
        UUID id,
        String name,
        String category,
        Integer wipLimit,
        String position,
        Integer taskCount) {
    }

    public record BoardMemberResponse(
        UUID id,
        String fullName,
        String avatarUrl,
        String initials,
        String role,
        Boolean isOwner) {
    }

    public record BoardPermissionResponse(Boolean canManageStructure, Boolean canCreateTask, Boolean canArchiveBoard) {
    }
}
