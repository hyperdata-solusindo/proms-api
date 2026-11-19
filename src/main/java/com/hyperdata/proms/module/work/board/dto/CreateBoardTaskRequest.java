package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBoardTaskRequest(
    String title,
    UUID workstreamId,
    UUID columnId,
    List<UUID> assigneeIds,
    LocalDate dueDate,
    String priority) {

    public static final Validator<CreateBoardTaskRequest> validator = ValidatorBuilder.<CreateBoardTaskRequest>of()
        .constraint(CreateBoardTaskRequest::title, "title", c -> c
            .notBlank()
            .message("Judul task wajib diisi")
            .lessThanOrEqual(255)
            .message("Judul task maksimal 255 karakter"))
        .constraintOnObject(CreateBoardTaskRequest::workstreamId, "workstreamId", c -> c
            .notNull()
            .message("Workstream wajib dipilih"))
        .constraint(CreateBoardTaskRequest::priority, "priority", c -> c
            .predicate(
                p -> p == null || List.of("low", "medium", "high", "urgent").contains(p),
                ViolationMessage.of("priority.invalid", "Priority tidak valid. Pilihan: low, medium, high, urgent")))
        .build();
}
