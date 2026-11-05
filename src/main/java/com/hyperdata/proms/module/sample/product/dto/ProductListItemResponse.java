package com.hyperdata.proms.module.sample.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductListItemResponse(UUID id, String sku, String name, BigDecimal price, int stock) {
}
