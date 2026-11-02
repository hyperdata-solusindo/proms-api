package com.hyperdata.proms.module.sample.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id, String sku, String name, BigDecimal price, int stock, boolean active, Instant createdAt) {}
