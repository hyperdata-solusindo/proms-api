package com.hyperdata.proms.module.pm.standup.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;

public record CreateStandupRequest(
    String plan,
    String done,
    String blockers) {

    public static final Validator<CreateStandupRequest> validator = ValidatorBuilder.<CreateStandupRequest>of()
        .constraint(CreateStandupRequest::plan, "plan", c -> c
            .notBlank()
            .message("Plan wajib diisi")
            .lessThanOrEqual(2000)
            .message("Plan maksimal 2000 karakter"))
        .constraint(CreateStandupRequest::done, "done", c -> c
            .notBlank()
            .message("Done wajib diisi")
            .lessThanOrEqual(2000)
            .message("Done maksimal 2000 karakter"))
        .constraint(CreateStandupRequest::blockers, "blockers", c -> c
            .lessThanOrEqual(2000)
            .message("Blockers maksimal 2000 karakter"))
        .build();
}
