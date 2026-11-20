package com.hyperdata.proms.module.work.client.dto;

import java.util.UUID;

public record TaskBoardColumnCountDto(UUID boardId, UUID columnId, long count) {

}
