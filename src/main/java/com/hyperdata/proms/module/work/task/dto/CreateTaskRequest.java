package com.hyperdata.proms.module.work.task.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import com.hyperdata.proms.module.work.task.constant.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Payload pembuatan task baru (stepper New Task / Quick add)")
public record CreateTaskRequest(
    @Schema(
        description = "Judul task",
        example = "Ganti kabel sensor",
        maxLength = TaskLimits.TITLE_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED)
    String title,

    @Schema(
        description = "UUID workstream tujuan. Quick add memakai workstream default board",
        example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c7d",
        requiredMode = Schema.RequiredMode.REQUIRED)
    UUID workstreamId,

    @Schema(
        description = "UUID kolom awal. Kosong = kolom pertama board",
        example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c80")
    UUID columnId,

    @Schema(description = "Daftar UUID user yang di-assign")
    List<UUID> assigneeIds,

    @Schema(description = "Tanggal jatuh tempo", example = "2026-12-01", format = "date")
    LocalDate dueDate,

    @Schema(
        description = "Prioritas task",
        example = TaskPriority.MEDIUM,
        allowableValues = {
            TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.URGENT})
    String priority,

    @Schema(description = "UUID milestone", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c81")
    UUID milestoneId,

    @Schema(description = "Dokumen editor deskripsi (format JSON yang sama dengan web)")
    Map<String, Object> description,

    @Schema(description = "Item checklist awal (langkah B3 Instructions)")
    List<ChecklistItemInput> checklists,

    @Schema(description = "Subtask awal (langkah B2 Details)")
    List<SubtaskInput> subtasks){

    @Schema(description = "Item checklist yang dibuat bersama task")
    public record ChecklistItemInput(
        @Schema(
            description = "Teks item checklist",
            example = "Kalibrasi sensor",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String text,
        @Schema(description = "Status selesai", example = "false")
        Boolean done) {
    }

    @Schema(description = "Subtask yang dibuat bersama task")
    public record SubtaskInput(
        @Schema(description = "Judul subtask", example = "Cek konektor", requiredMode = Schema.RequiredMode.REQUIRED)
        String title) {
    }

    private static final Validator<ChecklistItemInput> CHECKLIST_ITEM_VALIDATOR = ValidatorBuilder
        .<ChecklistItemInput>of()
        .constraint(ChecklistItemInput::text, "text", c -> c
            .notBlank()
            .message("Teks checklist wajib diisi")
            .lessThanOrEqual(TaskLimits.TITLE_MAX_LENGTH)
            .message("Teks checklist maksimal " + TaskLimits.TITLE_MAX_LENGTH + " karakter"))
        .build();

    private static final Validator<SubtaskInput> SUBTASK_VALIDATOR = ValidatorBuilder.<SubtaskInput>of()
        .constraint(SubtaskInput::title, "title", c -> c
            .notBlank()
            .message("Judul subtask wajib diisi")
            .lessThanOrEqual(TaskLimits.TITLE_MAX_LENGTH)
            .message("Judul subtask maksimal " + TaskLimits.TITLE_MAX_LENGTH + " karakter"))
        .build();

    public static final Validator<CreateTaskRequest> validator = ValidatorBuilder.<CreateTaskRequest>of()
        .constraint(CreateTaskRequest::title, "title", c -> c
            .notBlank()
            .message("Judul task wajib diisi")
            .lessThanOrEqual(TaskLimits.TITLE_MAX_LENGTH)
            .message("Judul task maksimal " + TaskLimits.TITLE_MAX_LENGTH + " karakter"))
        .constraintOnObject(CreateTaskRequest::workstreamId, "workstreamId", c -> c
            .notNull()
            .message("Workstream wajib dipilih"))
        .constraint(CreateTaskRequest::priority, "priority", c -> c
            .predicate(
                p -> p == null || TaskPriority.ALL.contains(p),
                ViolationMessage.of(
                    "priority.invalid",
                    "Priority tidak valid. Pilihan: " + String.join(", ", TaskPriority.ALL))))
        .forEachIfPresent(CreateTaskRequest::checklists, "checklists", CHECKLIST_ITEM_VALIDATOR)
        .forEachIfPresent(CreateTaskRequest::subtasks, "subtasks", SUBTASK_VALIDATOR)
        .build();
}
