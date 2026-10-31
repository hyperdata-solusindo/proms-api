package com.hyperdata.proms.module.sample.product.repository;

import com.hyperdata.proms.module.sample.product.entity.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    // Level 1: Derived method (1 column)
    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    // Level 2: Explicit @Query JPQL (complex criteria)
    @Query(
            """
                SELECT p FROM Product p
                WHERE p.active = true
                  AND p.stock >= :minStock
                ORDER BY p.name ASC
            """)
    List<Product> findActiveAvailableProducts(@Param("minStock") int minStock);
}
