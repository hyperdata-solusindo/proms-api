# Rule 05: Repository & JPA 3-Level Query Standards

Repositories extend `JpaRepository<Entity, ID>`. 
Khusus entitas agregat/utama yang memiliki fitur pencarian, filter dinamis, atau listing tabel (ERP Search Tables), **wajib** menambahkan `JpaSpecificationExecutor<Entity>`. Untuk entitas utilitas/internal sederhana (misal token session, tabel pivot sederhana), cukup meng-extend `JpaRepository<Entity, ID>`.

---

## The 3-Level Query Rules

Aturan ini dibuat untuk mencegah terciptanya *Monster Method Name* (deretan nama method panjang berisi instruksi SQL teknis) dan memastikan setiap query memiliki efisiensi serta keterbacaan yang tinggi.

### Level 1: Derived Query Methods (Maksimal 1–2 Atribut Sederhana)
Digunakan **hanya** untuk pengecekan keberadaan atau pencarian identitas unik berbasis 1–2 atribut langsung entitas:
- Contoh valid: `existsBySku(String sku)`, `findByEmail(String email)`, `findByBoardIdAndUserId(UUID boardId, UUID userId)`.

#### Batasan Mutlak Level 1 (Strict Invariants — Enforced by ArchUnit):
1. **DILARANG menyisipkan klausa `OrderBy...` pada nama method derived:**
   - ❌ `findByBoardIdOrderByPositionAsc(UUID boardId)`
   - ❌ `findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(UUID boardId)`
   - ✅ **Solusi A (Sorting Dinamis)**: Oper parameter `Sort` atau `Pageable` Spring Data: `findByBoardId(UUID boardId, Sort sort)`.
   - ✅ **Solusi B (Sorting Aturan Bisnis)**: Jika pengurutan merupakan aturan bisnis tetap, naik ke Level 2 (`@Query` JPQL) dengan nama Ubiquitous Language: `findActiveColumnsByBoardId(UUID boardId)`.
2. **DILARANG menyisipkan kondisi soft-delete (`DeletedAtIsNull` / `DeletedAtIsNotNull`) pada nama method derived:**
   - ❌ `findByIdAndDeletedAtIsNull(UUID id)`
   - ❌ `findByBoardIdAndDeletedAtIsNull(UUID boardId)`
   - ❌ `findByIdAndBoardIdAndDeletedAtIsNull(UUID id, UUID boardId)`
   - Soft-delete adalah aturan domain ("hanya ambil data aktif"). Menggabungkannya ke derived method name menciptakan *Monster Method Name* teknis.
   - ✅ **Wajib**: Gunakan Level 2 (`@Query`) dengan nama domain bisnis (misal `findActiveById`, `findActiveColumnsByBoardId`), atau gunakan JPA Specification.
3. **Maksimal 2 Atribut Filter (Maksimal 1 kata `And`):**
   - ❌ `findByIdAndBoardIdAndStatus(UUID id, UUID boardId, String status)` (3 atribut / 2 `And`)
   - ✅ **Wajib**: Jika melibatkan 3 atau lebih kriteria filter, gunakan Level 2 (`@Query` JPQL) atau Level 3 (`Specification`).
4. **DILARANG menyisipkan klausa limit/first majemuk:**
   - ❌ `findFirstByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status)`
   - ✅ **Wajib**: Gunakan Level 2 (`@Query` JPQL) dengan nama domain: `findLatestActiveByTenantId(UUID tenantId, String status)`.

---

### Level 2: Explicit `@Query` JPQL (> 2 Parameter, Soft-Delete, Sorting Bisnis, atau JOIN FETCH)
Wajib digunakan jika query memenuhi salah satu kriteria berikut:
1. **Menyaring data aktif / soft-delete** (`deletedAt IS NULL`).
2. **Memiliki aturan pengurutan bisnis tetap** (`ORDER BY ... ASC/DESC`).
3. **Memiliki parameter filter > 2** atau kombinasi logika kompleks.
4. **Membutuhkan pembatasan hasil** (`LIMIT 1` / data terbaru).
5. **Membutuhkan `JOIN FETCH`** antar-relasi internal modul untuk mencegah masalah N+1.

#### Standar Penulisan Level 2:
- **Java Text Blocks**: Selalu tulis query JPQL dalam format multi-baris rapi menggunakan `"""..."""`.
- **Ubiquitous Language**: Nama method **wajib** mencerminkan maksud bisnis domain (contoh: `findActiveColumnsByBoardId`, `findLatestActiveByTenantId`, `findMembership`), **BUKAN** deretan kata kunci SQL teknis.
- **Eksplisit `@Param`**: Setiap parameter pada JPQL wajib dianotasi `@Param("namaParam")`.

---

### Level 3: Dynamic Filters with `Specification` (ERP Search Tables)
Khusus endpoint pencarian dengan banyak filter opsional (misal: pencarian berdasarkan kata kunci nama, status, rentang tanggal, atau kategori):
1. Definisikan fungsi spesifikasi murni di `repository/spec/<Feature>Specs.java`.
2. Komposisikan dengan `Specification.where(...).and(...)` di layer Service.
3. Eksekusi via `repository.findAll(spec, pageable)`.
4. **Zero Magic Strings pada Predikat Kriteria**: Dilarang menulis string literal manual untuk nilai status, role, atau tab (misal: `cb.equal(root.get("status"), "active")`). Selalu gunakan konstanta domain dari `constant/` (contoh: `cb.equal(root.get("status"), BoardStatus.ACTIVE)`).

---

## Code Examples: DOs and DON'Ts

### 1. Board & Column Repository (Real Case Anti-Pattern)

```java
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    // ❌ FORBIDDEN: Monster derived method menggabungkan filter + soft-delete + OrderBy
    List<BoardColumn> findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(UUID boardId);

    // ❌ FORBIDDEN: Monster derived method dengan 3 filter kriteria (id, boardId, deletedAt)
    Optional<BoardColumn> findByIdAndBoardIdAndDeletedAtIsNull(UUID id, UUID boardId);

    // ✅ DO (Level 2): Gunakan @Query JPQL dengan Text Blocks dan nama Ubiquitous Language
    @Query("""
        SELECT c FROM BoardColumn c
        WHERE c.boardId = :boardId
          AND c.deletedAt IS NULL
        ORDER BY c.position ASC
    """)
    List<BoardColumn> findActiveColumnsByBoardId(@Param("boardId") UUID boardId);

    // ✅ DO (Level 2): Gunakan @Query JPQL untuk kueri identitas aktif terisolasi
    @Query("""
        SELECT c FROM BoardColumn c
        WHERE c.id = :id
          AND c.boardId = :boardId
          AND c.deletedAt IS NULL
    """)
    Optional<BoardColumn> findActiveColumnById(
            @Param("id") UUID id,
            @Param("boardId") UUID boardId
    );
}
```

---

### 2. Subscription & Tenant Repository

```java
public interface SubscriptionRepository
        extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {

    // ❌ FORBIDDEN: Monster derived method name dengan > 2 klausa (filter + sorting + limit)
    Optional<Subscription> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);

    // ✅ DO (Level 2): Beralih ke @Query JPQL dengan nama Ubiquitous Language
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.tenantId = :tenantId
          AND s.status = :status
        ORDER BY s.createdAt DESC
        LIMIT 1
    """)
    Optional<Subscription> findLatestActiveByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("status") String status
    );

    // ❌ FORBIDDEN: Menulis klausul OrderBy di nama method Level 1 jika hanya butuh sorting
    List<Subscription> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    // ✅ DO (Level 1): Oper parameter Sort bawaan Spring Data untuk fleksibilitas di Level 1
    List<Subscription> findByTenantId(UUID tenantId, Sort sort);
}
```

---

### 3. Parameter Binding & Formatting pada `@Query`

```java
public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    // ❌ DON'T: Query satu baris yang panjang atau konkatenasi string manual tanpa @Param
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock >= :minStock ORDER BY p.name ASC")
    List<Product> getProducts(int minStock);

    // ✅ DO: Gunakan Java Text Blocks, indentasi rapi, nama jelas, dan @Param eksplisit
    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND p.stock >= :minStock
        ORDER BY p.name ASC
    """)
    List<Product> findActiveAvailableProducts(@Param("minStock") int minStock);
}
```

---

### 4. Batasan Modul & Relasi Database (Modular Monolith)

```java
public interface OrderRepository
        extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    // ❌ DON'T: Melakukan SQL JOIN atau JOIN FETCH lintas tabel milik modul lain
    // (Melanggar aturan isolasi 2-Layer Modular Monolith & Rule 11 The Golden Stack)
    @Query("""
        SELECT o FROM Order o
        JOIN platform.users u ON o.userId = u.id
        WHERE u.email = :email
    """)
    List<Order> findOrdersByUserEmail(@Param("email") String email);

    // ✅ DO: Query lokal modul saja. Untuk data lintas modul, gunakan The Golden Stack (Lookups)
    @Query("""
        SELECT o FROM Order o
        WHERE o.userId = :userId
        ORDER BY o.createdAt DESC
    """)
    List<Order> findRecentOrdersByUserId(@Param("userId") UUID userId);
}
```

---

### 5. Projections & Aggregations (Zero `Object[]` Anti-Pattern)

Dilarang keras mengembalikan tipe array mentah `Object[]` atau `List<Object[]>` dari method repository.

```java
public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {

    // ❌ FORBIDDEN: Mengembalikan Object[] atau List<Object[]> mentah
    // Anti-pattern: Rawan ClassCastException, ArrayIndexOutOfBoundsException, dan NPE saat unboxing ((Number) row[1]).intValue()
    @Query("SELECT bm.boardId, COUNT(bm.id) FROM BoardMember bm WHERE bm.boardId IN :boardIds GROUP BY bm.boardId")
    List<Object[]> countMembersGroupedByBoardIds(@Param("boardIds") Collection<UUID> boardIds);

    // ✅ DO: Wajib gunakan Java 21 Record DTO Projection (via JPQL Constructor Expression)
    @Query("""
        SELECT new com.hyperdata.proms.module.pm.board.dto.BoardMemberCountDto(bm.boardId, COUNT(bm.id))
        FROM BoardMember bm
        WHERE bm.boardId IN :boardIds
        GROUP BY bm.boardId
    """)
    List<BoardMemberCountDto> countMembersGroupedByBoardIds(@Param("boardIds") Collection<UUID> boardIds);

    // ✅ DO: Proyeksi kolom majemuk menggunakan Record DTO
    @Query("""
        SELECT new com.hyperdata.proms.module.pm.board.dto.BoardMemberRoleDto(bm.boardId, bm.role)
        FROM BoardMember bm
        WHERE bm.userId = :userId AND bm.boardId IN :boardIds
    """)
    List<BoardMemberRoleDto> findRolesByUserAndBoardIds(
            @Param("userId") UUID userId,
            @Param("boardIds") Collection<UUID> boardIds
    );
}
```

#### Aturan Mutlak Agregasi & Proyeksi:
1. **Zero `Object[]` Policy**: Dilarang menggunakan `Object[]` di repository.
2. **Java 21 Record DTOs**: Buat record DTO di package `module.<domain>.<feature>.dto` (contoh: `BoardMemberCountDto`, `BoardMemberRoleDto`).
3. **JPQL Constructor Expression**: Gunakan `SELECT new com.hyperdata.proms...MyDto(...)` pada query JPQL Level 2.
4. **Type-Safe In-Memory Indexing**: Di layer service, indeks ke Map dengan aman menggunakan `Lookups.toMap(...)` dan method reference (contoh: `BoardMemberCountDto::boardId`), tanpa casting manual `(UUID) row[0]`.

---

### 6. Dynamic Criteria Specification & Predicates (Zero Magic Strings)

Pada layer JPA Specification (`repository/spec/<Feature>Specs.java`), predikat kriteria dilarang keras menggunakan string literal manual untuk perbandingan status, tab, atau role.

```java
public final class BoardSpecs {
    // ❌ DON'T: Hardcoded magic strings in criteria predicates
    if ("archived".equalsIgnoreCase(tab)) {
        predicates = cb.and(predicates, cb.isNotNull(root.get("archivedAt")));
    } else if ("shared".equalsIgnoreCase(tab)) {
        predicates = cb.and(predicates, cb.equal(root.get("status"), "active"));
        memberSubquery.where(cb.notEqual(memberRoot.get("role"), "admin"));
    }

    // ✅ DO: Always use centralized constants from constant/
    if (BoardTab.ARCHIVED.equalsIgnoreCase(tab)) {
        predicates = cb.and(predicates, cb.isNotNull(root.get("archivedAt")));
    } else if (BoardTab.SHARED.equalsIgnoreCase(tab)) {
        predicates = cb.and(predicates, cb.equal(root.get("status"), BoardStatus.ACTIVE));
        memberSubquery.where(cb.notEqual(memberRoot.get("role"), BoardMemberRole.ADMIN));
    }
}
```

| Area | ❌ DON'T (Dilarang) | ✅ DO (Wajib) |
| :--- | :--- | :--- |
| **Tab Filter Branching** | `if ("archived".equalsIgnoreCase(tab))` | `if (BoardTab.ARCHIVED.equalsIgnoreCase(tab))` |
| **Status Equal Criteria** | `cb.equal(root.get("status"), "active")` | `cb.equal(root.get("status"), BoardStatus.ACTIVE)` |
| **Role Exclusion Criteria** | `cb.notEqual(memberRoot.get("role"), "admin")` | `cb.notEqual(memberRoot.get("role"), BoardMemberRole.ADMIN)` |

---

### 7. Automated Enforcement (ArchUnit)

Aturan repositori ini dijaga secara otomatis oleh pengujian arsitektur di `ArchitectureTest.java`:
1. `repository_methods_without_query_must_not_contain_order_by`: Memastikan derived query method tidak mengandung `OrderBy`.
2. `repository_methods_without_query_must_not_contain_soft_delete_clauses`: Memastikan derived query method tidak mengandung `DeletedAt`.
3. `repository_methods_without_query_must_not_exceed_two_filter_criteria`: Memastikan derived query method tidak memiliki lebih dari 1 kata `And` (maksimal 2 filter kriteria).
4. `repository_methods_must_not_return_raw_object_arrays`: Memastikan tidak ada method repository yang mengembalikan array mentah `Object[]` atau `List<Object[]>`.

Setiap pelanggaran aturan di atas akan langsung menggagalkan `./gradlew test --tests "*ArchitectureTest*"` dan `./gradlew check`.
