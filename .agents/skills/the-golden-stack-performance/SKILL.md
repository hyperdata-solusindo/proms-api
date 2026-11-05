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
         UserDto user = userFacade.findById(order.userId());
     }

     // DO: The Golden Stack (Batch lookup + in-memory Map)
     Set<UUID> userIds = orders.stream().map(OrderDto::userId).collect(toSet());
     Map<UUID, UserDto> userLookups = userFacade.findByIdsIn(userIds);
     List<OrderResponse> responses = orders.stream()
         .map(o -> mapper.toResponse(o, userLookups.get(o.userId())))
         .toList();
     ```

2. **JPA Entity Restrictions**:
   - **NEVER use `@Data`** on JPA Entities (causes infinite `hashCode`/`equals` loops and accidental mutation).
   - Use explicit `@Getter`, `@Setter`, and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
   - All `@ManyToOne` or `@OneToOne` associations must specify `fetch = FetchType.LAZY`.
   - Never expose JPA Entities to Web Controllers or Facades (Anti-Entity Leakage).

3. **Transaction Management**:
   - Read-only methods must be annotated with `@Transactional(readOnly = true)`.
   - `@Transactional` is STRICTLY FORBIDDEN on Controller classes and Controller methods.

## Review Output Format for The Golden Stack
- **N+1 & Cross-Module Queries**: [PASS / VIOLATION] (Explanation)
- **JPA Annotations & Lombok**: [PASS / VIOLATION] (Explanation)
- **Transaction Scope**: [PASS / VIOLATION] (Explanation)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
