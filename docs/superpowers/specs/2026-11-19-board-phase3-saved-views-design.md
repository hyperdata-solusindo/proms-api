# Spesifikasi Desain Arsitektur: Modul Board Fase 3 (Custom Saved Views)

- **Tanggal Dokumen**: 2026-11-19
- **Domain & Modul**: `com.hyperdata.proms.module.work.board`
- **Referensi Desain**: Dokumen `mobile-v1-api-mapping-spring` (Figma Mobile `07-list`, `07-board`) & `board-endpoints-flow-and-relations.md`
- **Status**: Draft untuk Review User

---

## 1. Ringkasan Eksekutif & Tujuan

Spesifikasi ini mendefinisikan implementasi **Fase 3** dari modul Board di domain `work`. Fase 3 berfokus pada manajemen tampilan filter tersimpan (*Custom Saved Views*):
1. **API-67**: `GET /api/v1/boards/{boardId}/views` (Mengambil daftar saved view kustom milik user atau view default board).
2. **API-68**: `POST /api/v1/boards/{boardId}/views` (Menyimpan preset filter/sort baru sebagai view kustom).

---

## 2. Kepatuhan Master Directives (AGENTS.md)

1. **Internal Domain Feature**:
   - Seluruh data saved views berada dalam satu kesatuan boundary modul `work.board` (tabel `proms.board_saved_views`). Tidak memerlukan panggilan client ke modul lain.
2. **PostgreSQL RLS & JSONB**:
   - Entitas `BoardSavedView` memanfaatkan kolom JSONB (`filters` dan `sort_config`) yang dipetakan menggunakan `@JdbcTypeCode(SqlTypes.JSON)`.
   - Menggunakan UUIDv7 untuk sequential index dan `@TenantScoped` / `tenant_id` untuk isolasi tenant.
3. **Idempotency & Concurrency**:
   - Pembuatan view baru (`POST`) diproteksi dengan header `Idempotency-Key`.
   - Kolom `updated_at` / `version` disertakan dalam response untuk mendukung optimistik concurrency saat pengeditan di masa depan.

---

## 3. Data Model & Persistensi (JPA Entity)

### Entitas: `BoardSavedView`
Memetakan tabel PostgreSQL `proms.board_saved_views`:

```java
package com.hyperdata.proms.module.work.board.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "board_saved_views", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class BoardSavedView {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "view_type", nullable = false)
    private String viewType; // list, board, timeline, calendar

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> filters = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sort_config", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> sortConfig = Map.of();

    @Column(name = "group_by")
    private String groupBy;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
```

---

## 4. Spesifikasi Kontrak DTO & Endpoint

### A. API-67: `GET /api/v1/boards/{boardId}/views` (Daftar Saved Views)
- **Path**: `GET /api/v1/boards/{boardId}/views`
- **Logika Eksekusi**:
  - Ambil saved views di mana `tenant_id = :tenantId AND board_id = :boardId AND (user_id = :userId OR is_default = true)` diurutkan berdasarkan `position ASC`.
- **Response**: `RestResponse<List<SavedViewResponse>>`
```java
public record SavedViewResponse(
    UUID id,
    String name,
    String type,                     // "list" atau "board"
    Map<String, Object> filters,     // e.g. {"workstreamId": "...", "priority": "high", "overdue": true}
    Map<String, Object> sortConfig,
    String groupBy,
    boolean isDefault,
    int position,
    String version
) {}
```

### B. API-68: `POST /api/v1/boards/{boardId}/views` (Simpan View Baru)
- **Path**: `POST /api/v1/boards/{boardId}/views`
- **Header**: `Idempotency-Key: <UUID>` (Wajib)
- **Request Record**: `CreateSavedViewRequest`
```java
public record CreateSavedViewRequest(
    String name,
    String type,                     // "list" | "board" | "timeline" | "calendar"
    Map<String, Object> filters,
    Map<String, Object> sortConfig,
    String groupBy,
    Boolean isDefault
) {
    public static final Validator<CreateSavedViewRequest> VALIDATOR = ValidatorBuilder.<CreateSavedViewRequest>of()
        .constraint(CreateSavedViewRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .constraint(CreateSavedViewRequest::type, "type", c -> c.notBlank())
        .build();
}
```
- **Logika Eksekusi**:
  1. Jika `isDefault == true`, reset flag `is_default` pada view lain milik user/board tersebut agar indeks unik `board_saved_views_one_default` tidak melanggar constraint.
  2. Hitung `position` otomatis (max position + 1).
  3. Simpan ke database PostgreSQL.
- **Response**: `201 Created` $\rightarrow$ `RestResponse<SavedViewResponse>`

