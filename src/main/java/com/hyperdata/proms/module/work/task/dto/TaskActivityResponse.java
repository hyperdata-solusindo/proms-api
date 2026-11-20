package com.hyperdata.proms.module.work.task.dto;

import com.hyperdata.proms.module.work.client.dto.TaskUserDto;
import com.hyperdata.proms.module.work.task.constant.TaskActivityAction;
import com.hyperdata.proms.module.work.task.constant.TaskActivityField;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Satu baris riwayat perubahan task")
public record TaskActivityResponse(
    @Schema(description = "ID activity (bigint, dipakai juga sebagai cursor)", example = "1024")
    Long id,
    @Schema(description = "User yang melakukan aksi (null bila sistem)")
    TaskUserDto actor,
    @Schema(
        description = "Jenis aksi",
        example = TaskActivityAction.UPDATED,
        allowableValues = {
            TaskActivityAction.CREATED, TaskActivityAction.UPDATED, TaskActivityAction.COMMENTED})
    String action,
    @Schema(description = "Field yang berubah", example = TaskActivityField.COLUMN)
    String field,
    @Schema(description = "Nilai lama (JSON)", example = "{\"name\": \"To do\"}")
    Object oldValue,
    @Schema(description = "Nilai baru (JSON)", example = "{\"name\": \"In progress\"}")
    Object newValue,
    @Schema(description = "Waktu aksi", format = "date-time")
    Instant createdAt){

}
