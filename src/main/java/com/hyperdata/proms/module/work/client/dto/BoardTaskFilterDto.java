package com.hyperdata.proms.module.work.client.dto;

import java.util.UUID;

public record BoardTaskFilterDto(
    UUID workstreamId,
    UUID assigneeId,
    String searchKeyword,
    Boolean sharedWithClientOnly,
    Integer limit,
    String cursor) {
}
