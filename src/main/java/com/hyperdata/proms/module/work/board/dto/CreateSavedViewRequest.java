package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Payload penyimpanan custom filter view baru")
public record CreateSavedViewRequest(
    @Schema(
        description = "Nama view tersimpan",
        example = "Overdue hardware",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(
        description = "Tipe view (list, board, timeline, calendar)",
        example = "list",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String type,
    @Schema(description = "Konfigurasi kriteria filter JSON")
    Map<String, Object> filters,
    @Schema(description = "Konfigurasi pengurutan JSON")
    Map<String, Object> sortConfig,
    @Schema(description = "Pengelompokan (group by)")
    String groupBy,
    @Schema(description = "Jadikan sebagai default view", example = "false")
    Boolean isDefault) {

    public static final Validator<CreateSavedViewRequest> VALIDATOR = ValidatorBuilder.<CreateSavedViewRequest>of()
        .constraint(CreateSavedViewRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .constraint(CreateSavedViewRequest::type, "type", c -> c.notBlank())
        .build();
}
