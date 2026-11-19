---
name: the-golden-stack-performance
description: Audits JPA entities, database repositories, lookups, and mappers for Anti-N+1 compliance, L1 cache enrichment, lazy loading, and readOnly transactions.
---

# The Golden Stack & JPA Performance Review Skill

Use this specialized skill when reviewing changes involving repositories, entities, mappers, in-memory lookups, caching, or database queries.

## Mandatory Audit Invariants (Rule 5, 10, & 11)

1. **Anti-N+1 Cross-Module Enrichment (The Golden Stack Pattern)**:
   - **NO SQL JOIN across modular boundaries**: Tables belonging to different domains must not be joined via SQL or `@ManyToOne` entity relations.
   - **NO queries inside loops**:
     ```java
     // DON'T: N+1 query vulnerability!
     for (OrderDto order : orders) {
         UserDto user = userClient.findById(order.userId());
     }

     // DO: The Golden Stack (Batch lookup + in-memory Map)
     Set<UUID> userIds = orders.stream().map(OrderDto::userId).collect(toSet());
     Map<UUID, UserDto> userLookups = userClient.findByIdsIn(userIds);
     List<OrderResponse> responses = orders.stream()
         .map(o -> mapper.toResponse(o, userLookups.get(o.userId())))
         .toList();
     ```

2. **JPA Entity & Primary Key Restrictions (Rule 5 & 9)**:
   - **Primary Key UUIDv7**: Primary key WAJIB menggunakan UUIDv7 via Hibernate 7 `@Id @UuidGenerator(style = UuidGenerator.Style.VERSION_7)`. Dilarang menggunakan UUIDv4 acak untuk primary key database.
   - **Entity Lifecycle Auditing**: Kolom audit (`createdAt`, `updatedAt`) dikelola secara deterministik via metode `@PrePersist` dan `@PreUpdate` pada entity.
   - **NEVER use `@Data`** on JPA Entities (causes infinite `hashCode`/`equals` loops and accidental mutation).
   - Use explicit `@Getter`, `@Setter`, and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
   - All `@ManyToOne` or `@OneToOne` associations must specify `fetch = FetchType.LAZY`.
   - Never expose JPA Entities to Web Controllers or Client Contracts (Anti-Entity Leakage).
   - **JPA 3-Level Query Standard (Rule 9)**: Tier 1 (Derived Query) -> Tier 2 (JPQL/HQL `@Query` dengan DTO Projection) -> Tier 3 (JPA Specification `CriteriaBuilder` untuk filter dinamis).

3. **Transaction Management**:
   - Read-only methods must be annotated with `@Transactional(readOnly = true)`.
   - `@Transactional` is STRICTLY FORBIDDEN on Controller classes and Controller methods.

## Review Output Format for The Golden Stack
- **N+1 & Cross-Module Queries**: [PASS / VIOLATION] (Explanation)
- **JPA Annotations & Lombok**: [PASS / VIOLATION] (Explanation)
- **Transaction Scope**: [PASS / VIOLATION] (Explanation)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
