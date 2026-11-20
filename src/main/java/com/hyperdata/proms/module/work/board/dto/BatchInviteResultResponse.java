package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Respon batch hasil pengiriman undangan")
public record BatchInviteResultResponse(
    @Schema(description = "Rincian status hasil per email")
    List<InviteItemResultDto> results) {
}
