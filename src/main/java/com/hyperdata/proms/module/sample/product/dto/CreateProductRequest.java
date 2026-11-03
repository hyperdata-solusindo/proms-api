package com.hyperdata.proms.module.sample.product.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import java.math.BigDecimal;

public record CreateProductRequest(String sku, String name, BigDecimal price, int stock) {

    public static final Validator<CreateProductRequest> validator = ValidatorBuilder.<CreateProductRequest>of()
        .constraint(CreateProductRequest::sku, "sku", c -> c.notBlank()
            .message("SKU is required")
            .lessThanOrEqual(64)
            .message("SKU max 64 chars"))
        .constraint(CreateProductRequest::name, "name", c -> c.notBlank()
            .message("Product name is required")
            .lessThanOrEqual(128)
            .message("Name max 128 chars"))
        .constraint(CreateProductRequest::price, "price", c -> c.notNull()
            .message("Price is required")
            .greaterThan(BigDecimal.ZERO)
            .message("Price must be greater than zero"))
        .constraint(CreateProductRequest::stock, "stock", c -> c.greaterThanOrEqual(0)
            .message("Stock cannot be negative"))
        .build();
}
