package com.hyperdata.proms.module.sample.product.repository.spec;

import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecs {

    private ProductSpecs() {}

    public static Specification<Product> of(ProductFilter filter) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (filter == null) {
            return spec;
        }
        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and(nameContains(filter.name().trim()));
        }
        if (filter.sku() != null && !filter.sku().isBlank()) {
            spec = spec.and(skuEquals(filter.sku().trim()));
        }
        if (filter.active() != null) {
            spec = spec.and(filter.active() ? isActive() : isInactive());
        }
        return spec;
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> skuEquals(String sku) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("sku")), sku.toLowerCase());
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> isInactive() {
        return (root, query, cb) -> cb.isFalse(root.get("active"));
    }
}
