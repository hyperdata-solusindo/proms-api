package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Representasi saved view kustom pada board")
public record SavedViewResponse(
    UUID id,
    String name,
    String type,
    Map<String, Object> filters,
    Map<String, Object> sortConfig,
    String groupBy,
    boolean isDefault,
    int position,
    String version) {
}
