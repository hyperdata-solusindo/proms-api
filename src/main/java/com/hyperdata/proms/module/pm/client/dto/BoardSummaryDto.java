package com.hyperdata.proms.module.pm.client.dto;

import java.util.UUID;

public record BoardSummaryDto(UUID id, String key, String name, String status) {
}
