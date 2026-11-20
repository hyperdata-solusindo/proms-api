package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Payload pembuatan board baru")
public record CreateBoardRequest(
    @Schema(description = "Nama board", example = "Nusa Bioscience IoT", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(
        description = "Kode unik board (2-5 karakter kapital)",
        example = "NBI",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String key,
    @Schema(description = "Nama klien", example = "PT Nusa Bioscience")
    String clientName,
    @Schema(description = "Nama grup divisi/kategori", example = "Development")
    String groupName,
    @Schema(description = "UUID Project Manager")
    UUID pmId,
    @Schema(description = "Tanggal mulai board", example = "2026-11-20")
    LocalDate startDate,
    @Schema(description = "Tanggal jatuh tempo board", example = "2026-12-20")
    LocalDate dueDate,
    @Schema(description = "UUID Template Alur Kerja yang dipilih")
    UUID templateId,
    @Schema(description = "Nilai kontrak (Rupiah)", example = "50000000")
    Long contractValue) {

    public static final Validator<CreateBoardRequest> VALIDATOR = ValidatorBuilder.<CreateBoardRequest>of()
        .constraint(CreateBoardRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .constraint(CreateBoardRequest::key, "key", c -> c.notBlank().pattern("^[A-Z0-9]{2,5}$"))
        .constraintOnTarget(
            req -> req.startDate() == null || req.dueDate() == null || !req.dueDate().isBefore(req.startDate()),
            "dueDate",
            ViolationMessage.of("dueDate.invalid", "startDate tidak boleh lebih besar dari dueDate"))
        .build();

    public static final Validator<CreateBoardRequest> validator = VALIDATOR;
}
