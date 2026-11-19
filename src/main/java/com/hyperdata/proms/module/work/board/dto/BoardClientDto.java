package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Informasi klien board")
public record BoardClientDto(
    @Schema(description = "UUID klien")
    UUID id,
    @Schema(description = "Nama klien", example = "PT Nusa Bioscience")
    String name) {
}
