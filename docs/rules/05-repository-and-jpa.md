# Rule 05: Repository & JPA 3-Level Query Standards

Repositories extend `JpaRepository<Entity, UUID>` and `JpaSpecificationExecutor<Entity>`.

## The 3-Level Query Rules

### Level 1: Derived Query Methods (1–2 simple fields)
Used for primary key checks, unique lookups, or simple existence checks.
```java
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);
}
```

### Level 2: Explicit `@Query` JPQL (> 2 parameters or JOIN FETCH)
Used when query requires JOINs to prevent N+1 queries, complex date intervals, or projections.
- Always use Java Text Blocks `"""..."""`.
- Name methods with clear business meaning.
- Avoid derived method names longer than 3 clauses.

```java
@Query("""
    SELECT o FROM Order o
    JOIN FETCH o.customer c
    JOIN FETCH o.items i
    WHERE o.customer.id = :customerId
      AND o.status = :status
      AND o.orderDate BETWEEN :startDate AND :endDate
    ORDER BY o.orderDate DESC
""")
List<Order> findCustomerOrdersWithDetails(
    @Param("customerId") UUID customerId,
    @Param("status") OrderStatus status,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

### Level 3: Dynamic Filters with `Specification` (ERP Search Tables)
For search endpoints with optional query filters (e.g. search by name, category, date range, or price).
1. Define pure specification functions in `repository/spec/<Feature>Specs.java`.
2. Compose with `Specification.where(...).and(...)` in Service layer.
3. Pass to `repository.findAll(spec, pageable)`.
