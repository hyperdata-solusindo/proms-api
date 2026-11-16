# Rule 02: Coding Standards & Idioms

## 1. Java 21 Records for DTOs
All Request and Response objects MUST be Java 21 `record`.
- Immutable by default
- Zero boilerplate (no getters/setters needed)
- Compact constructor or static factory support
- Clean Jackson serialization

## 2. JPA Entity Standards (Lombok & UUIDv7 Rules)
JPA Entities MUST use Lombok annotations carefully and use **UUIDv7** for sequential B-tree index performance:
```java
@Entity
@Table(name = "customers", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Customer {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
```

### Primary Key (ID) Generation Standards:
- **UUIDv7 (Mandatory for Business Entities)**: Seluruh entitas bisnis berbasis UUID **WAJIB** menggunakan `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` dari Hibernate 7 (RFC 9562 time-ordered UUID). DILARANG menggunakan `@GeneratedValue(strategy = GenerationType.UUID)` karena secara default menghasilkan UUIDv4 (acak) yang memicu fragmentasi indeks B-tree di PostgreSQL.
- **Natural Key Exception**: Entitas konfigurasi 1-to-1 per tenant (seperti `StandupPolicies`) diperbolehkan menggunakan `@Id @Column(name = "tenant_id") private UUID tenantId;` tanpa generator.
- **Identity BigInt Exception**: Entitas performa tinggi atau session utility (seperti `PersonalAccessToken`) diperbolehkan menggunakan `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`.

### Lifecycle Auditing (`@PrePersist` & `@PreUpdate`):
- Seluruh entitas dengan kolom timestamp audit `created_at` dan `updated_at` **WAJIB** menggunakan lifecycle callback `@PrePersist` dan `@PreUpdate`.
- DILARANG hanya mengandalkan inisialisasi inline (`private Instant updatedAt = Instant.now();`) tanpa setter/callback, karena JPA tidak akan memperbarui nilai `updatedAt` saat mutasi update, yang berakibat merusak validasi optimistik locking `ConcurrencyGuard` (`If-Match`).

### PROHIBITED on JPA Entities:
- ❌ **NEVER use `@Data`** on JPA Entities (causes infinite `hashCode()`/`toString()` recursion on bi-directional relationships and breaks lazy loading).
- ❌ **NEVER use `@EqualsAndHashCode`** including all fields. Only compare primary key or unique business key.

## 3. Dependency Injection
- **ALWAYS** use **Constructor Injection** using Lombok `@RequiredArgsConstructor` on classes with `private final` fields.
- ❌ **NEVER use Field Injection** (`@Autowired private MyService myService;`).

## 4. Modern Java 21 Idioms
- Use `var` for local variables with obvious types.
- Use Pattern Matching for `switch` and `instanceof` (`if (obj instanceof String s)`).
- Use Text Blocks (`"""..."""`) for multiline strings and JPQL queries.
- Use `java.time` (`Instant`, `LocalDate`, `ZonedDateTime`). NEVER use `java.util.Date` or `java.util.Calendar`.

## 5. Zero Unused Code & Clean Dependencies (YAGNI & Dead Code Prohibition)
- ❌ **DILARANG mendeklarasikan variabel, field, parameter, method, atau import yang tidak pernah digunakan (Dead Code)**.
- Setiap field `private final` yang diinjeksi via `@RequiredArgsConstructor` **wajib** digunakan secara nyata di dalam method class.
- ❌ **DILARANG menginjeksi dependensi spekulatif** (*"untuk jaga-jaga di masa depan"*). Tambahkan dependensi (seperti `Repository`, `Service`, atau `Client`) hanya saat fiturnya benar-benar diimplementasikan.
- Pastikan kode bersih dari compiler/IDE warning (seperti `The value of the field ... is not used` atau `The method ... is never used locally`).

## 6. Strict Explicit Imports & Strict No-FQCN Rule (Enforced by Checkstyle)
- ❌ **DILARANG KERAS menuliskan Fully Qualified Class Name (FQCN)** di dalam kode Java (misal: `com.hyperdata.proms.common.response.RestResponse.class` atau `java.util.UUID`).
- Seluruh kelas dari library atau paket lain **WAJIB di-import secara eksplisit** di header file (`import ...;`).
- **Enforcement Otomatis di Pipeline**: Aturan ini ditegakkan secara ketat pada level AST compiler via **Checkstyle** (`config/checkstyle/checkstyle.xml`). Pelanggaran penggunaan FQCN internal (`com.hyperdata...`) di luar klausa `import` atau `package` akan langsung menggagalkan build (`maxErrors = 0`).
- **Pengecualian Tunggal**: FQCN hanya diizinkan jika terjadi *class name collision* (konflik nama kelas yang identik) dari dua package berbeda dalam file yang sama (misalnya `java.util.Date` vs `java.sql.Date`). Di luar kasus tabrakan nama tersebut, FQCN dianggap sebagai pelanggaran standar kode (*code smell*).

## 7. Zero Magic Strings (Centralized Constants for Status, Roles, Types, and Tabs)
- ❌ **DILARANG KERAS menuliskan magic strings / string literal langsung di kode logika bisnis, repository spec, entity default, client, service, atau controller** (contoh pelanggaran: `"archived"`, `"active"`, `"owner"`, `"viewer"`, `"admin"`, `"lead"`).
- Seluruh nilai status, roles, tabs, tipe kategori, atau state machine **WAJIB didefinisikan secara terpusat** di dalam package `constant/` pada feature modul terkait (misal: `BoardStatus`, `BoardMemberRole`, `BoardTab`) atau sebagai `Enum` jika memiliki perilaku/state.
- Class konstanta harus berupa `public final class` dengan **private constructor** dan `public static final String` constant fields.
- Nilai konstanta tersebut wajib digunakan di seluruh layer:
  - Default value entity: `@Column(name = "status") private String status = BoardStatus.ACTIVE;`
  - Validasi & IF checks: `if (BoardStatus.ARCHIVED.equalsIgnoreCase(board.getStatus()))`
  - Fallback / Ternary roles: `isOwner ? BoardMemberRole.OWNER : BoardMemberRole.VIEWER`
  - JPA Specification: `predicates = cb.and(predicates, cb.equal(root.get("status"), BoardStatus.ACTIVE));`
  - OpenAPI / Controller default values: `@RequestParam(value = "tab", defaultValue = BoardTab.MINE)`

### DOs and DON'Ts — Magic Strings & Constants

| Area | ❌ DON'T (Dilarang Keras) | ✅ DO (Wajib Dilakukan) |
| :--- | :--- | :--- |
| **Status Check** | `"archived".equalsIgnoreCase(board.getStatus())` | `BoardStatus.ARCHIVED.equalsIgnoreCase(board.getStatus())` |
| **Role Assignment & Fallback** | `orElse(isOwner ? "owner" : "viewer")` | `orElse(isOwner ? BoardMemberRole.OWNER : BoardMemberRole.VIEWER)` |
| **Permission / Structure Check** | `"lead".equalsIgnoreCase(role) \|\| "admin".equalsIgnoreCase(role)` | `BoardMemberRole.LEAD.equalsIgnoreCase(role) \|\| BoardMemberRole.ADMIN.equalsIgnoreCase(role)` |
| **JPA Criteria Specification** | `cb.equal(root.get("status"), "active")` | `cb.equal(root.get("status"), BoardStatus.ACTIVE)` |
| **JPA Subquery Filter** | `cb.notEqual(memberRoot.get("role"), "admin")` | `cb.notEqual(memberRoot.get("role"), BoardMemberRole.ADMIN)` |
| **Entity Default Field** | `private String status = "active";` | `private String status = BoardStatus.ACTIVE;` |
| **Controller Annotations** | `@RequestParam(defaultValue = "mine")` | `@RequestParam(defaultValue = BoardTab.MINE)` |
| **Constant Class Declaration** | `public interface BoardConstants { ... }` (Anti-pattern: constant interface leak) | `public final class BoardStatus { private BoardStatus() {} public static final String ACTIVE = "active"; }` |

