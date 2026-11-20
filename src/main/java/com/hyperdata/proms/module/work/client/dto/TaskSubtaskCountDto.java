package com.hyperdata.proms.module.work.client.dto;

import java.util.UUID;

public record TaskSubtaskCountDto(UUID parentId, long done, long total) {

}
