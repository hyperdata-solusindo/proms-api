package com.hyperdata.proms.module.sample.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryDto(UUID id, String sku, String name, BigDecimal price, boolean isAvailable) {
}
