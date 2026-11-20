package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Payload undangan member board")
public record InviteBoardMembersRequest(
    @Schema(description = "Daftar email yang diundang", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> emails,
    @Schema(
        description = "Role yang diberikan (admin, pm, member, viewer, guest)",
        example = "member",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String role,
    @Schema(description = "Daftar UUID workstream yang dialokasikan")
    List<UUID> workstreamIds) {

    public static final Validator<InviteBoardMembersRequest> VALIDATOR = ValidatorBuilder
        .<InviteBoardMembersRequest>of()
        .constraint(InviteBoardMembersRequest::emails, "emails", c -> c.notEmpty())
        .constraint(InviteBoardMembersRequest::role, "role", c -> c.notBlank())
        .build();

    public static final Validator<InviteBoardMembersRequest> validator = VALIDATOR;
}
