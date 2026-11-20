package com.hyperdata.proms.module.work.task.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Payload pemindahan task ke kolom lain (Taskmove)")
public record MoveTaskRequest(
    @Schema(
        description = "UUID kolom tujuan",
        example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c80",
        requiredMode = Schema.RequiredMode.REQUIRED)
    UUID columnId,

    @Schema(description = "Taruh sebelum task ini (null di mobile)")
    UUID beforeTaskId,

    @Schema(description = "Taruh sesudah task ini (null di mobile)")
    UUID afterTaskId,

    @Schema(
        description = "Alasan blocked, wajib bila kolom tujuan berkategori blocked",
        example = "Menunggu suplai sensor")
    String blockedReason) {

    public static final Validator<MoveTaskRequest> validator = ValidatorBuilder.<MoveTaskRequest>of()
        .constraintOnObject(MoveTaskRequest::columnId, "columnId", c -> c
            .notNull()
            .message("Kolom tujuan wajib dipilih"))
        .constraint(MoveTaskRequest::blockedReason, "blockedReason", c -> c
            .lessThanOrEqual(TaskLimits.BLOCKED_REASON_MAX_LENGTH)
            .message("Alasan blocked maksimal " + TaskLimits.BLOCKED_REASON_MAX_LENGTH + " karakter"))
        .build();
}
