package com.hyperdata.proms.module.pm.standup.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;

public record UpdateStandupRequest(
    String plan,
    String done,
    String blockers) {

    public static final Validator<UpdateStandupRequest> validator = ValidatorBuilder.<UpdateStandupRequest>of()
        .constraintOnTarget(
            r -> r.plan() != null || r.done() != null || r.blockers() != null,
            "request",
            "request.empty",
            "Minimal satu field harus diisi")
        .constraint(UpdateStandupRequest::plan, "plan", c -> c
            .predicate(
                p -> p == null || !p.isBlank(),
                ViolationMessage.of("plan.blank", "Plan tidak boleh kosong"))
            .lessThanOrEqual(2000)
            .message("Plan maksimal 2000 karakter"))
        .constraint(UpdateStandupRequest::done, "done", c -> c
            .predicate(
                d -> d == null || !d.isBlank(),
                ViolationMessage.of("done.blank", "Done tidak boleh kosong"))
            .lessThanOrEqual(2000)
            .message("Done maksimal 2000 karakter"))
        .constraint(UpdateStandupRequest::blockers, "blockers", c -> c
            .lessThanOrEqual(2000)
            .message("Blockers maksimal 2000 karakter"))
        .build();
}
