# Rule 05: Repository & JPA 3-Level Query Standards

Repositories extend `JpaRepository<Entity, ID>`. 
Khusus entitas agregat/utama yang memiliki fitur pencarian, filter dinamis, atau listing tabel (ERP Search Tables), **wajib** menambahkan `JpaSpecificationExecutor<Entity>`. Untuk entitas utilitas/internal sederhana (misal token session, tabel pivot sederhana), cukup meng-extend `JpaRepository<Entity, ID>`.

---

## The 3-Level Query Rules

### Level 1: Derived Query Methods (1–2 Field Sederhana)
Digunakan hanya untuk pengecekan keberadaan, lookup identitas unik, atau filter 1–2 kolom sederhana tanpa kombinasi klausa kompleks.
- Contoh: `existsBySku(String sku)`, `findByEmail(String email)`.
- Jika membutuhkan pengurutan sederhana tanpa klausa tambahan, oper parameter `Sort` atau `Pageable` daripada merangkai nama fungsi yang panjang.

### Level 2: Explicit `@Query` JPQL (> 2 Parameter, Sorting/Limit Kompleks, atau JOIN FETCH)
Wajib digunakan jika query:
1. Memiliki parameter > 2.
2. Menggabungkan kondisi filter dengan pengurutan (`ORDER BY`) dan pembatasan (`LIMIT` / `findFirst`).
3. Membutuhkan `JOIN FETCH` antar-relasi internal modul untuk mencegah masalah N+1.
- Selalu gunakan Java Text Blocks `"""..."""`.
- Wajib memberi nama method yang mencerminkan **tujuan bisnis (Ubiquitous Language)**, bukan deretan kata kunci SQL.
- Hindari merangkai nama derived method yang memiliki lebih dari 2 klausa gabungan.

### Level 3: Dynamic Filters with `Specification` (ERP Search Tables)
Khusus endpoint pencarian dengan banyak filter opsional (misal: pencarian berdasarkan kata kunci nama, status, rentang tanggal, atau kategori):
1. Definisikan fungsi spesifikasi murni di `repository/spec/<Feature>Specs.java`.
2. Komposisikan dengan `Specification.where(...).and(...)` di layer Service.
3. Eksekusi via `repository.findAll(spec, pageable)`.

---

## Code Examples: DOs and DON'Ts

### 1. Naming & Method Complexity (Derived vs `@Query`)

```java
public interface SubscriptionRepository
        extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {

    // ❌ DON'T: Monster derived method name dengan > 2 klausa (kombinasi filter + sorting + limit)
    // Nama method 51 karakter, sulit dibaca, dan mengekspos instruksi SQL teknis daripada makna bisnis
    Optional<Subscription> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);

    // ✅ DO: Beralih ke Level 2 (@Query JPQL) dengan Java text blocks dan nama bermakna bisnis
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

    // ❌ DON'T: Menulis klausul OrderBy panjang di nama method Level 1 jika hanya butuh sorting
    List<Subscription> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    // ✅ DO: Oper parameter Sort bawaan Spring Data untuk fleksibilitas di Level 1
    List<Subscription> findByTenantId(UUID tenantId, Sort sort);
}
```

---

### 2. Parameter Binding & Formatting pada `@Query`

```java
public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    // ❌ DON'T: Query satu baris yang panjang atau konkatenasi string manual
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock >= :minStock ORDER BY p.name ASC")
    List<Product> getProducts(int minStock); // nama terlalu generik dan tanpa @Param eksplisit

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

### 3. Batasan Modul & Relasi Database (Modular Monolith)

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

### 4. Ekstensi Interface Repositori

```java
// ❌ DON'T: Mengabaikan JpaSpecificationExecutor pada entitas agregat yang membutuhkan search table
public interface ProductRepository extends JpaRepository<Product, UUID> {
    // Kurang JpaSpecificationExecutor, sehingga tidak bisa memproses filter dinamis Specification
}

// ✅ DO: Extend JpaSpecificationExecutor pada entitas agregat yang mendukung pencarian dinamis
public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
}

// ✅ DO: Repositori utilitas internal sederhana cukup extend JpaRepository
public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {
    Optional<PersonalAccessToken> findByIdAndToken(Long id, String token);
}
```
