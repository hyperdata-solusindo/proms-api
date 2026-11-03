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
