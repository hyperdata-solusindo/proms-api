package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Payload pembaruan metadata board")
public record UpdateBoardRequest(
    @Schema(description = "Nama board", example = "Nusa Bioscience IoT v2", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(description = "Deskripsi detail board")
    String description,
    @Schema(description = "Nama klien", example = "PT Nusa Bioscience")
    String clientName,
    @Schema(description = "Nama grup divisi/kategori", example = "Development")
    String groupName,
    @Schema(description = "UUID Project Manager")
    UUID pmId,
    @Schema(description = "Tanggal mulai board", example = "2026-11-20")
    LocalDate startDate,
    @Schema(description = "Tanggal jatuh tempo board", example = "2026-12-20")
    LocalDate dueDate) {

    public UpdateBoardRequest(String name, String description) {
        this(name, description, null, null, null, null, null);
    }
    public static final Validator<UpdateBoardRequest> VALIDATOR = ValidatorBuilder.<UpdateBoardRequest>of()
        .constraint(UpdateBoardRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .constraintOnTarget(
            req -> req.startDate() == null || req.dueDate() == null || !req.dueDate().isBefore(req.startDate()),
            "dueDate",
            ViolationMessage.of("dueDate.invalid", "startDate tidak boleh lebih besar dari dueDate"))
        .build();

    public static final Validator<UpdateBoardRequest> validator = VALIDATOR;
}
