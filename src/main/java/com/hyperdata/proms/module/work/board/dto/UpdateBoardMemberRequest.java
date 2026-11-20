package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Payload pembaruan role atau workstream member board")
public record UpdateBoardMemberRequest(
    @Schema(description = "Role baru pada board", example = "member")
    String role,
    @Schema(description = "Daftar UUID workstream yang dialokasikan")
    List<UUID> workstreamIds) {

    public static final Validator<UpdateBoardMemberRequest> VALIDATOR = ValidatorBuilder.<UpdateBoardMemberRequest>of()
        .build();

    public static final Validator<UpdateBoardMemberRequest> validator = VALIDATOR;
}
