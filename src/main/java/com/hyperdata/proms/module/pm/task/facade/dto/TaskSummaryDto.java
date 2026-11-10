package com.hyperdata.proms.module.pm.task.facade.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskSummaryDto(
    UUID id,
    String key,
    UUID boardId,
    String boardName,
    UUID columnId,
    String category,
    String title,
    String priority,
    LocalDate dueDate,
    String dueState,
    List<AssigneeRefDto> assignees,
    boolean isBlocked,
    int subtaskDone,
    int subtaskTotal,
    int commentCount,
    Instant version) {

    public record AssigneeRefDto(UUID id, String fullName, String initials) {
    }
}
