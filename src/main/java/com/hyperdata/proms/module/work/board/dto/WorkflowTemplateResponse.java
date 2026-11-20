package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Representasi template alur kerja kanban")
public record WorkflowTemplateResponse(
    @Schema(description = "UUID Template", example = "00000000-0000-0000-0000-000000000001")
    UUID id,
    @Schema(description = "Nama template workflow", example = "Default Workflow")
    String name,
    @Schema(description = "Deskripsi template workflow", example = "Standar kanban 4 kolom")
    String description,
    @Schema(description = "Apakah template bawaan/default sistem", example = "true")
    boolean isDefault,
    @Schema(description = "Daftar kolom kanban bawaan pada template")
    List<WorkflowTemplateColumnDto> columns) {

    public record WorkflowTemplateColumnDto(
        @Schema(description = "Nama kolom", example = "Backlog")
        String name,
        @Schema(description = "Kategori kolom (not_started, in_progress, blocked, done)", example = "not_started")
        String category,
        @Schema(description = "Kode warna hex kolom", example = "#1E293B")
        String color) {
    }
}
