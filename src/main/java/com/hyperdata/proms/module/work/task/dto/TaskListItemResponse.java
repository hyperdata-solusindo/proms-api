package com.hyperdata.proms.module.work.task.dto;

import com.hyperdata.proms.common.docs.annotation.VersionEpochSchema;
import com.hyperdata.proms.module.work.client.dto.TaskUserDto;
import com.hyperdata.proms.module.work.task.constant.TaskCategory;
import com.hyperdata.proms.module.work.task.constant.TaskDueDate;
import com.hyperdata.proms.module.work.task.constant.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ringkasan task untuk daftar (TaskSummary)")
public record TaskListItemResponse(
    @Schema(description = "UUID task", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c01")
    UUID id,
    @Schema(description = "Kode task {boardKey}-{number}", example = "NB-12")
    String key,
    @Schema(description = "UUID board", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c70")
    UUID boardId,
    @Schema(description = "Nama board", example = "Nusa Bioscience")
    String boardName,
    @Schema(description = "UUID kolom saat ini", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c80")
    UUID columnId,
    @Schema(description = "Kategori kolom", example = TaskCategory.IN_PROGRESS)
    String category,
    @Schema(description = "Judul task", example = "Integrasi sensor suhu")
    String title,
    @Schema(description = "Prioritas", example = TaskPriority.HIGH)
    String priority,
    @Schema(description = "Tanggal jatuh tempo", example = "2026-10-28", format = "date")
    LocalDate dueDate,
    @Schema(
        description = "Status jatuh tempo dihitung server",
        example = TaskDueDate.SOON,
        allowableValues = {
            TaskDueDate.LATE, TaskDueDate.SOON, TaskDueDate.OK, TaskDueDate.NONE})
    String dueState,
    @Schema(description = "Jumlah hari terlambat (0 bila tidak terlambat)", example = "0")
    int daysLate,
    @Schema(description = "Assignee task")
    List<TaskUserDto> assignees,
    @Schema(description = "Task sedang blocked", example = "false")
    boolean isBlocked,
    @Schema(description = "Jumlah subtask selesai", example = "1")
    int subtaskDone,
    @Schema(description = "Total subtask", example = "3")
    int subtaskTotal,
    @Schema(description = "Jumlah komentar", example = "4")
    int commentCount,
    @VersionEpochSchema
    Instant version){
}
