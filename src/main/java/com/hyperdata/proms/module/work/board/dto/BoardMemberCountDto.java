package com.hyperdata.proms.module.work.board.dto;

import java.util.UUID;

public record BoardMemberCountDto(UUID boardId, long count) {
}
