package com.hyperdata.proms.module.work.task.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import com.hyperdata.proms.module.work.task.constant.TaskDueFilter;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import com.hyperdata.proms.module.work.task.constant.TaskPriority;
import com.hyperdata.proms.module.work.task.constant.TaskStatusFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Filter daftar task yang di-assign ke user aktif")
public record MyTaskFilter(
    @Schema(description = "Batasi ke satu board", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c70")
    UUID boardId,

    @Schema(
        description = "Segmen jatuh tempo, dihitung dengan zona waktu tenant",
        example = TaskDueFilter.TODAY,
        allowableValues = {
            TaskDueFilter.TODAY, TaskDueFilter.OVERDUE, TaskDueFilter.UPCOMING, TaskDueFilter.ALL})
    String due,

    @Schema(
        description = "Prioritas task",
        example = TaskPriority.HIGH,
        allowableValues = {TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.URGENT})
    String priority,

    @Schema(
        description = "Kategori kolom task",
        example = TaskStatusFilter.IN_PROGRESS,
        allowableValues = {TaskStatusFilter.TODO, TaskStatusFilter.IN_PROGRESS, TaskStatusFilter.REVIEW,
            TaskStatusFilter.BLOCKED})
    String status,

    @Schema(description = "Sertakan task di kolom done", example = "false")
    Boolean includeDone,

    @Schema(description = "Jumlah item per halaman", example = "50", maximum = "200")
    Integer limit,

    @Schema(description = "Cursor halaman berikutnya dari meta.nextCursor", example = "eyJvIjo1MH0")
    String cursor){

    private static final List<String> DUE_VALUES = List.of(
        TaskDueFilter.TODAY,
        TaskDueFilter.OVERDUE,
        TaskDueFilter.UPCOMING,
        TaskDueFilter.ALL);

    private static final List<String> STATUS_VALUES = List.of(
        TaskStatusFilter.TODO,
        TaskStatusFilter.IN_PROGRESS,
        TaskStatusFilter.REVIEW,
        TaskStatusFilter.BLOCKED);

    public static final Validator<MyTaskFilter> validator = ValidatorBuilder.<MyTaskFilter>of()
        .constraint(MyTaskFilter::due, "due", c -> c
            .predicate(
                d -> d == null || DUE_VALUES.contains(d),
                ViolationMessage.of("due.invalid",
                    "Filter due tidak valid. Pilihan: " + String.join(", ", DUE_VALUES))))
        .constraint(MyTaskFilter::priority, "priority", c -> c
            .predicate(
                p -> p == null || TaskPriority.ALL.contains(p),
                ViolationMessage.of(
                    "priority.invalid",
                    "Priority tidak valid. Pilihan: " + String.join(", ", TaskPriority.ALL))))
        .constraint(MyTaskFilter::status, "status", c -> c
            .predicate(
                s -> s == null || STATUS_VALUES.contains(s),
                ViolationMessage.of(
                    "status.invalid",
                    "Status tidak valid. Pilihan: " + String.join(", ", STATUS_VALUES))))
        .constraint(MyTaskFilter::limit, "limit", c -> c
            .greaterThanOrEqual(1)
            .message("Limit minimal 1")
            .lessThanOrEqual(TaskLimits.MAX_PAGE_SIZE)
            .message("Limit maksimal " + TaskLimits.MAX_PAGE_SIZE))
        .build();
}
