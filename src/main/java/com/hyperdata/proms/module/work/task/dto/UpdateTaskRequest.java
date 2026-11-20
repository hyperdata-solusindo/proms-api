package com.hyperdata.proms.module.work.task.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import com.hyperdata.proms.module.work.task.constant.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Payload pembaruan sebagian task (TaskPatch). Field null = tidak diubah")
public record UpdateTaskRequest(
    @Schema(description = "Judul task", example = "Integrasi sensor suhu", maxLength = TaskLimits.TITLE_MAX_LENGTH)
    String title,

    @Schema(description = "Dokumen editor deskripsi (format JSON yang sama dengan web)")
    Map<String, Object> description,

    @Schema(
        description = "Prioritas task",
        example = TaskPriority.HIGH,
        allowableValues = {
            TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.URGENT})
    String priority,

    @Schema(
        description = "Waktu mulai. Pengisian pertama saat task di kolom not_started memindahkan task ke kolom in_progress pertama",
        example = "2026-11-20T02:00:00Z",
        format = "date-time")
    Instant startDate,

    @Schema(description = "Tanggal jatuh tempo", example = "2026-11-28", format = "date")
    LocalDate dueDate,

    @Schema(description = "UUID milestone", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c81")
    UUID milestoneId,

    @Schema(description = "Daftar lengkap UUID assignee pengganti (server menghitung diff)")
    List<UUID> assigneeIds){

    public static final Validator<UpdateTaskRequest> validator = ValidatorBuilder.<UpdateTaskRequest>of()
        .constraint(UpdateTaskRequest::title, "title", c -> c
            .predicate(
                t -> t == null || !t.isBlank(),
                ViolationMessage.of("title.blank", "Judul task tidak boleh kosong"))
            .lessThanOrEqual(TaskLimits.TITLE_MAX_LENGTH)
            .message("Judul task maksimal " + TaskLimits.TITLE_MAX_LENGTH + " karakter"))
        .constraint(UpdateTaskRequest::priority, "priority", c -> c
            .predicate(
                p -> p == null || TaskPriority.ALL.contains(p),
                ViolationMessage.of(
                    "priority.invalid",
                    "Priority tidak valid. Pilihan: " + String.join(", ", TaskPriority.ALL))))
        .build();
}
