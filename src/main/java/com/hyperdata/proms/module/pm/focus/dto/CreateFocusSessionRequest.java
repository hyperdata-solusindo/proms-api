package com.hyperdata.proms.module.pm.focus.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import java.time.Instant;
import java.util.UUID;

public record CreateFocusSessionRequest(
    Instant startedAt,
    Instant endedAt,
    UUID taskId) {

    public static final Validator<CreateFocusSessionRequest> validator = ValidatorBuilder
        .<CreateFocusSessionRequest>of()
        .constraintOnObject(CreateFocusSessionRequest::startedAt, "startedAt", c -> c
            .notNull()
            .message("Waktu mulai Wajib disiisi"))
        .constraintOnObject(CreateFocusSessionRequest::endedAt, "endedAt", c -> c
            .notNull()
            .message("Waktu selesai wajib diisi"))
        .build();

}
