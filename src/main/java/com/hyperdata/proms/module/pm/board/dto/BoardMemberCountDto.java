package com.hyperdata.proms.module.pm.board.dto;

import java.util.UUID;

public record BoardMemberCountDto(UUID boardId, long count) {
}
