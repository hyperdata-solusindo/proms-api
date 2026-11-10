package com.hyperdata.proms.module.pm.task.facade.dto;

import java.util.UUID;

public record BoardTaskFilterDto(
    UUID workstreamId,
    UUID assigneeId,
    String searchKeyword,
    Boolean sharedWithClientOnly,
    Integer limit,
    String cursor) {
}
