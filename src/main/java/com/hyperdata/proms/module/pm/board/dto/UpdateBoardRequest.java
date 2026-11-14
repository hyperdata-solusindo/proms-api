package com.hyperdata.proms.module.pm.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request untuk memperbarui informasi dasar board")
public record UpdateBoardRequest(
    @Schema(description = "Nama board", example = "E-Commerce Revamp 2026", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(description = "Deskripsi board", example = "Proyek modernisasi portal web")
    String description) {

    public static final Validator<UpdateBoardRequest> validator = ValidatorBuilder.<UpdateBoardRequest>of()
        .constraint(UpdateBoardRequest::name, "name", c -> c
            .notBlank()
            .message("Nama board wajib diisi")
            .lessThanOrEqual(255)
            .message("Nama board maksimal 255 karakter"))
        .build();
}
