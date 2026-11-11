package com.hyperdata.proms.module.pm.board.facade.dto;

import java.util.UUID;

public record BoardSummaryDto(UUID id, String key, String name, String status) {
}
