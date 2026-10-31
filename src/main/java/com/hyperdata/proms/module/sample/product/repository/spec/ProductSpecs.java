package com.hyperdata.proms.module.sample.product.repository.spec;

import com.hyperdata.proms.module.sample.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecs {

    private ProductSpecs() {}

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }
}
