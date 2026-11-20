package com.hyperdata.proms.module.work.client.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTaskCommand(
    String boardKey,
    UUID workstreamId,
    UUID columnId,
    String title,
    List<UUID> assigneeIds,
    LocalDate dueDate,
    String priority,
    UUID creatorId) {
}
