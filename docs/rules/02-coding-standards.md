# Rule 02: Coding Standards & Idioms

## 1. Java 21 Records for DTOs
All Request and Response objects MUST be Java 21 `record`.
- Immutable by default
- Zero boilerplate (no getters/setters needed)
- Compact constructor or static factory support
- Clean Jackson serialization

## 2. JPA Entity Standards (Lombok Rules)
JPA Entities MUST use Lombok annotations carefully:
```java
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // ...
}
```

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
- ❌ **DILARANG menginjeksi dependensi spekulatif** (*"untuk jaga-jaga di masa depan"*). Tambahkan dependensi (seperti `Repository`, `Service`, atau `Facade`) hanya saat fiturnya benar-benar diimplementasikan.
- Pastikan kode bersih dari compiler/IDE warning (seperti `The value of the field ... is not used` atau `The method ... is never used locally`).

## 6. Strict Explicit Imports & Strict No-FQCN Rule
- ❌ **DILARANG KERAS menuliskan Fully Qualified Class Name (FQCN)** di dalam kode Java (misal: `com.hyperdata.proms.common.response.RestResponse.class` atau `java.util.UUID`).
- Seluruh kelas dari library atau paket lain **WAJIB di-import secara eksplisit** di header file (`import ...;`).
- **Pengecualian Tunggal**: FQCN hanya diizinkan jika terjadi *class name collision* (konflik nama kelas yang identik) dari dua package berbeda dalam file yang sama (misalnya `java.util.Date` vs `java.sql.Date`). Di luar kasus tabrakan nama tersebut, FQCN dianggap sebagai pelanggaran standar kode (*code smell*).

## 7. Zero Magic Strings (Centralized Constants for Status, Roles, Types, and Tabs)
- ❌ **DILARANG KERAS menuliskan magic strings / string literal langsung di kode logika bisnis, repository spec, entity default, facade, service, atau controller** (contoh pelanggaran: `"archived"`, `"active"`, `"owner"`, `"viewer"`, `"admin"`, `"lead"`).
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

