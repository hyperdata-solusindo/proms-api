# Rule 16: Zero Magic Strings, Domain Constants & Localized Messages

## 1. Zero Magic Strings Principle
Dilarang keras (*strictly forbidden*) menyebarkan string literal mentah (*magic strings*) untuk:
1. Status domain (`"ACTIVE"`, `"ARCHIVED"`, `"COMPLETED"`).
2. Peran / hak akses pengguna (`"OWNER"`, `"ADMIN"`, `"MEMBER"`, `"VIEWER"`).
3. Tab navigasi atau tipe entitas (`"OVERVIEW"`, `"LIST"`, `"BOARD"`).
4. Kode error aplikasi (`"VALIDATION"`, `"RATE_LIMIT_EXCEEDED"`, `"RESOURCE_NOT_FOUND"`).
5. Pesan teks respons atau error yang ditujukan kepada pengguna/klien.

Seluruh string wajib dikapsulasi secara terpusat:
- Nilai status, role, dan tab $\rightarrow$ konstanta `public static final String` dalam `constant/<Feature>Status.java` / `constant/<Feature>Role.java` atau Java `enum`.
- Kode error teknis umum $\rightarrow$ `CommonErrorCode.java`.
- Pesan dinamis & multibahasa $\rightarrow$ `Messages.get("message.key", args)`.

---

## 2. Struktur Domain Constants (`constant/`)

Setiap domain fitur wajib memiliki package `constant/` jika memiliki status atau peran:
```text
module/<domain>/<feature>/constant/
├── <Feature>Status.java         # public final class dengan private constructor
├── <Feature>Role.java           # Hak akses / peran fitur
└── <Feature>CacheNames.java     # Nama-nama cache namespace fitur
```

### 2.1 Pola Deklarasi Class Constant
```java
package com.hyperdata.proms.module.pm.board.constant;

public final class BoardStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String ARCHIVED = "ARCHIVED";
    public static final String DELETED = "DELETED";

    private BoardStatus() {
        // Prevent instantiation
    }
}
```

### 2.2 Aturan Penggunaan Constant di Kode
- **Entity Default Values & Column Definitions**:
  ```java
  // DO
  @Column(name = "status", nullable = false)
  private String status = BoardStatus.ACTIVE;

  // DON'T: Magic string
  @Column(name = "status", nullable = false)
  private String status = "ACTIVE";
  ```
- **Service & Business Logic Checks**:
  ```java
  // DO
  if (BoardStatus.ARCHIVED.equals(board.getStatus())) {
      throw new BusinessException("BR-BD-02", Messages.get("board.error.archived"));
  }

  // DON'T: Magic string
  if ("ARCHIVED".equals(board.getStatus())) { ... }
  ```
- **YAVI Validation / Set Constraint**:
  ```java
  private static final Set<String> ALLOWED_STATUSES = Set.of(
      BoardStatus.ACTIVE,
      BoardStatus.ARCHIVED
  );

  public static final Validator<UpdateBoardRequest> validator = ValidatorBuilder.<UpdateBoardRequest>of()
      .constraint(UpdateBoardRequest::status, "status", c -> c
          .predicate(ALLOWED_STATUSES::contains, Messages.get("board.error.invalid_status")))
      .build();
  ```

---

## 3. Localized Messages via `Messages.java` (Multi-Language / i18n)

Semua pesan yang dikirim ke pengguna dalam `RestResponse.ok(...)`, `RestResponse.created(...)`, maupun exception **WAJIB** menggunakan helper terpusat `Messages.get(...)`:

```java
// Controller Success Messages
return RestResponse.ok(response, Messages.get("product.updated.success"));

// Controller List Retrieved
return RestResponse.ok(result, Messages.get("board.list.retrieved"));

// Exception with Dynamic Parameters
throw new ResourceNotFoundException(
    Messages.get("error.not_found.resource", "Board", boardId)
);
```

### 3.1 Resolusi Bahasa Otomatis
- Header HTTP `Accept-Language` dibaca otomatis oleh `Messages.resolveLocale()`:
  - `Accept-Language: en` $\rightarrow$ memuat pesan dari file `*_en.properties`.
  - Default / fallback $\rightarrow$ memuat pesan dari file `.properties` (Bahasa Indonesia `id_ID`).
- Lokasi file resource bundle:
  `src/main/resources/messages/<domain>/<feature>.properties` dan `<feature>_en.properties`.

---

## 4. Standar UUIDv7 Generator (Spring Boot + Hibernate 7)

Primary key entitas bertipe `UUID` wajib menggunakan generator **UUIDv7** bawaan Hibernate 7 (`org.hibernate.annotations.UuidGenerator`):

```java
// DO: Native Hibernate 7 UUIDv7 generator (RFC 9562 time-ordered)
@Id
@org.hibernate.annotations.UuidGenerator(style = org.hibernate.annotations.UuidGenerator.Style.VERSION_7)
private UUID id;

// DON'T: Random UUIDv4 generator (merusak efisiensi B-tree index PostgreSQL)
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

### 4.1 Mengapa Bukan JPA Murni `@GeneratedValue(strategy = GenerationType.UUID)`?
Standar spesifikasi `jakarta.persistence.GenerationType.UUID` hanya menghasilkan UUIDv4 (acak). Hibernate 7 menyediakan implementasi native `UuidGenerator.Style.VERSION_7` yang menyisipkan 48-bit timestamp Unix epoch di bit terdepan secara deterministik di memori sebelum baris disimpan ke database.

---

## 5. DOs & DON'Ts

- ✅ **DO**: Selalu gunakan `constant/<Feature>Status.java` atau `Enum` untuk nilai status/kategori.
- ✅ **DO**: Selalu bungkus pesan teks dengan `Messages.get("key.message")` agar mendukung multi-bahasa.
- ✅ **DO**: Gunakan `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` untuk semua primary key UUID baru.
- ❌ **DON'T**: Jangan pernah menulis raw string seperti `"ACTIVE"`, `"PENDING"`, `"Bearer "` tersebar di service atau controller.
- ❌ **DON'T**: Jangan menggunakan `@GeneratedValue(strategy = GenerationType.UUID)` karena menghasilkan UUIDv4 acak (ditolak otomatis oleh ArchUnit).
- ❌ **DON'T**: Jangan men-hardcode string pesan berbahasa Indonesia atau Inggris di kode Java; daftarkan kuncinya di `src/main/resources/messages/**/*.properties`.
