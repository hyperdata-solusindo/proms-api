package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Informasi grup divisi board")
public record BoardGroupDto(
    @Schema(description = "UUID grup")
    UUID id,
    @Schema(description = "Nama grup", example = "IoT")
    String name) {
}
