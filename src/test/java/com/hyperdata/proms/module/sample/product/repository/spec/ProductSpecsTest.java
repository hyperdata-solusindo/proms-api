package com.hyperdata.proms.module.sample.product.repository.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class ProductSpecsTest {

    @Test
    @DisplayName("Given null filter, when ProductSpecs.of, then return valid non-null Specification")
    void shouldReturnSpecificationWhenFilterNull() {
        Specification<Product> spec = ProductSpecs.of(null);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Given filter with name, sku, and active status, when ProductSpecs.of, then compose specifications")
    void shouldComposeSpecificationsFromFilter() {
        ProductFilter filter = new ProductFilter("Laptop", "SKU-001", true);
        Specification<Product> spec = ProductSpecs.of(filter);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Given filter with inactive status, when ProductSpecs.of, then compose isInactive specification")
    void shouldComposeInactiveSpecification() {
        ProductFilter filter = new ProductFilter(null, null, false);
        Specification<Product> spec = ProductSpecs.of(filter);
        assertNotNull(spec);
    }
}
