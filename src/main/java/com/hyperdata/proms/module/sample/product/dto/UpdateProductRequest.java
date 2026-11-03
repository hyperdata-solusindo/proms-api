package com.hyperdata.proms.module.sample.product.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import java.math.BigDecimal;

public record UpdateProductRequest(String name, BigDecimal price, int stock, Boolean active) {

    public static final Validator<UpdateProductRequest> validator = ValidatorBuilder.<UpdateProductRequest>of()
        .constraint(UpdateProductRequest::name, "name", c -> c.notBlank()
            .message("Product name is required")
            .lessThanOrEqual(128)
            .message("Name max 128 chars"))
        .constraint(UpdateProductRequest::price, "price", c -> c.notNull()
            .message("Price is required")
            .greaterThan(BigDecimal.ZERO)
            .message("Price must be greater than zero"))
        .constraint(UpdateProductRequest::stock, "stock", c -> c.greaterThanOrEqual(0)
            .message("Stock cannot be negative"))
        .build();
}
