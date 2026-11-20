# Spesifikasi Desain Arsitektur: Modul Board Fase 1 (Core Board, List & Card View)

- **Tanggal Dokumen**: 2026-11-19
- **Domain & Modul**: `com.hyperdata.proms.module.work.board`
- **Referensi Desain**: Dokumen `mobile-v1-api-mapping-spring` (Figma Mobile `07`, `07-card`, `07-add`, `07-shared`, `07-archive`, `07-list`, `07-board`) & `board-endpoints-flow-and-relations.md`
- **Status**: Draft untuk Review User

---

## 1. Ringkasan Eksekutif & Tujuan

Spesifikasi ini mendefinisikan implementasi **Fase 1** dari modul Board di domain `work`. Fase 1 berfokus pada fondasi entitas template alur kerja, siklus hidup board (*create, update, archive/soft-delete*), serta dua model penyajian data utama: **List View** ringkas dan **Card View Dashboard** kaya metrik (memuat matriks status task `To do`, `Doing`, `Blocked`, `Done`, progress bar, avatar member, dan hitungan mundur `daysLeft`).

### Scope Fase 1 (Endpoint):
1. **API-49**: `POST /api/v1/boards` (Buat Board baru + inisialisasi kolom dari template + assign PM/Creator)
2. **API-12**: `GET /api/v1/boards` (List ringkas board per tab `mine|shared|archived` + `meta.summary` + `meta.groups`)
3. **API-66**: `GET /api/v1/boards/cards` (Card view komprehensif + `taskCounts` + avatar member + `daysLeft`)
4. **API-13**: `GET /api/v1/boards/{boardId}` (Detail board + enrichment `columns[].taskCount`)
5. **API-50**: `PUT /api/v1/boards/{boardId}` (Update metadata board + concurrency guard `If-Match`)
6. **API-51**: `DELETE /api/v1/boards/{boardId}` (Arsip / Soft-Delete board + concurrency guard `If-Match`)

---

## 2. Kepatuhan Master Directives (AGENTS.md)

1. **2-Layer Modular Monolith**:
   - Seluruh kode berada di `com.hyperdata.proms.module.work.board` (private feature) dan `com.hyperdata.proms.module.work.client` (public contract).
2. **Inter-Module Client Contract**:
   - Berinteraksi dengan `UserClient` (`identity.user`), `TaskClient` (`work.task`), dan `TenantClient` (`platform.tenant`).
   - Dilarang keras melakukan foreign service injection atau SQL JOIN lintas skema (`proms.boards` $\leftrightarrow$ `platform.users`).
3. **No Entity Leakage & DTO Immutability**:
   - Seluruh request dan response menggunakan Java 21 `record`.
4. **No `@Data` pada Entities & UUIDv7 Mandatory**:
   - Menggunakan `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor(access = AccessLevel.PROTECTED)`.
   - Menggunakan `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` untuk ID bisnis.
5. **The Golden Stack (Anti-N+1 In-Memory Lookups)**:
   - Batching ID (`pmIds`, `boardIds`, `memberUserIds`) $\rightarrow$ Panggil Client method menerima `Collection<UUID>` $\rightarrow$ Mapping di memori menggunakan `Lookups.toMap(...)` $\rightarrow$ Assemble DTO.
6. **Zero Magic Strings & Health Direct Mapping**:
   - Menggunakan konstanta domain `BoardStatus`, `BoardHealth` (`ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `NEEDS_REVIEW`, `DONE`), `BoardTab`, dan `BoardCacheNames`.
   - Kolom `health` dan `healthReason` dibaca langsung dari tabel `proms.boards` tanpa kalkulasi paksa di backend.

---

## 3. Data Model & Persistensi (JPA Entities)

### A. Entitas Baru: `WorkflowTemplate` & `TemplateColumn`
Digunakan saat user membuat board baru (`POST /boards`) untuk menduplikasi kolom-kolom default ke `proms.board_columns`.

```java
package com.hyperdata.proms.module.work.board.entity;

@Entity
@Table(name = "workflow_templates", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowTemplate {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId; // null = global/system default template

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TemplateColumn> columns = new ArrayList<>();

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
}
```

```java
package com.hyperdata.proms.module.work.board.entity;

@Entity
@Table(name = "template_columns", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class TemplateColumn {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowTemplate template;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category; // not_started, in_progress, blocked, done

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "position", nullable = false)
    private String position;
}
```

### B. Penyempurnaan Entitas `Board`
Memastikan field-field sesuai tabel `proms.boards`:
- `health` (`String`), `healthReason` (`String`), `progressPct` (`Short`/`Integer`).
- `boardGroupId` (`UUID`), `groupName` (`String`), `clientName` (`String`), `pmId` (`UUID`), `ownerId` (`UUID`).
- `archivedAt` (`Instant`), `archivedBy` (`UUID`), `deletedAt` (`Instant`), `deletedBy` (`UUID`).

---

## 4. Spesifikasi Kontrak DTO & Endpoint

### A. API-49: `POST /api/v1/boards` (Create Board)
- **Header**: `Idempotency-Key: <UUID>` (Wajib)
- **Request Record**: `CreateBoardRequest`
  ```java
  public record CreateBoardRequest(
      String name,
      String key,
      String clientName,
      String groupName,
      UUID pmId,
      LocalDate startDate,
      LocalDate dueDate,
      UUID templateId,
      Long contractValue
  ) {
      public static final Validator<CreateBoardRequest> VALIDATOR = ValidatorBuilder.<CreateBoardRequest>of()
          .constraint(CreateBoardRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
          .constraint(CreateBoardRequest::key, "key", c -> c.notBlank().pattern("^[A-Z0-9]{2,5}$"))
          .build();
  }
  ```
- **Response**: `201 Created` $\rightarrow$ `RestResponse<BoardDetailResponse>`

### B. API-12: `GET /api/v1/boards` (List Ringkas)
- **Query Params**: `tab` (`mine|shared|archived`), `groupId` (UUID opsional), `q` (search), `cursor`, `limit` (default 20).
- **Response**: `RestResponse<List<BoardSummaryResponse>>` yang dibungkus dengan `meta`:
  ```json
  {
    "statusCode": 200,
    "message": "Berhasil",
    "data": [
      {
        "id": "UUID",
        "key": "NB",
        "name": "Nusa Bioscience",
        "groupName": "Development",
        "status": "active",
        "health": "on_track",
        "healthReason": null,
        "progressPct": 64,
        "dueDate": "2026-12-15",
        "pm": { "id": "UUID", "fullName": "Dewi S" },
        "memberCount": 8,
        "myRole": "member",
        "version": "1731920000000"
      }
    ],
    "meta": {
      "nextCursor": null,
      "summary": { "activeBoards": 12, "onTrack": 10, "atRisk": 2 },
      "groups": [
        { "id": "g-1", "name": "Design", "count": 1 },
        { "id": "g-2", "name": "Development", "count": 0 }
      ]
    }
  }
  ```

### C. API-66: `GET /api/v1/boards/cards` (Card View Dashboard)
- **Query Params**: Sama dengan API-12.
- **Response**: `RestResponse<List<BoardCardResponse>>`
  ```java
  public record BoardCardResponse(
      UUID id,
      String key,
      String name,
      BoardGroupDto group,
      BoardClientDto client,
      String health,
      String healthReason,
      long activeTaskCount,
      BoardTaskCountsDto taskCounts,
      long doneCount,
      long totalCount,
      int progressPct,
      LocalDate dueDate,
      Long daysLeft,
      List<BoardMemberAvatarDto> members,
      int memberCount,
      String version
  ) {}

  public record BoardTaskCountsDto(long todo, long doing, long blocked, long done) {}
  public record BoardMemberAvatarDto(UUID id, String initials, String avatarUrl) {}
  public record BoardGroupDto(UUID id, String name) {}
  public record BoardClientDto(UUID id, String name) {}
  ```

### D. API-13: `GET /api/v1/boards/{boardId}` (Detail Board)
- Memperkaya response `BoardDetailResponse.ColumnDetailResponse` dengan field:
  - `long taskCount` (didapat dari `TaskClient.getColumnTaskCounts(boardId)`).

### E. API-50: `PUT /api/v1/boards/{boardId}` (Update Board)
- **Header**: `If-Match: <version>` (Wajib)
- **Request Record**: `UpdateBoardRequest`
  - Mendukung update: `name`, `description`, `clientName`, `groupName`, `pmId`, `startDate`, `dueDate`.
- **Response**: `RestResponse<BoardDetailResponse>`.

### F. API-51: `DELETE /api/v1/boards/{boardId}` (Arsip / Soft Delete)
- **Header**: `If-Match: <version>` (Wajib)
- **Logika**:
  - Mengisi `archivedAt = now()`, `archivedBy = currentUser.userId()`.
  - Jika param `permanent=true` (khusus admin/owner), isi `deletedAt = now()`.
- **Response**: `RestResponse<Void>` (`statusCode: 200`, `data: null`).

---

## 5. Alur Data & Pola Integrasi (The Golden Stack)

### Pipeline Card View (`GET /boards/cards`):
1. **Query Database Lokal**:
   - `boardRepository.findActiveCards(tenantId, userId, tab, groupId)` (Derived Specification).
   - `boardMemberRepository.findTopMembersPerBoard(tenantId, boardIds, 3)`.
2. **Enrichment Lintas Modul (Batch)**:
   - **`TenantClient.getTenantDetail(tenantId)`**: Mengambil timezone tenant untuk kalkulasi:
     $$\text{daysLeft} = \text{ChronoUnit.DAYS.between}(\text{LocalDate.now(tenantZone)}, \text{dueDate})$$
   - **`TaskClient.getBoardTaskMetrics(boardIds)`**: Mengambil ringkasan task `{ todo, doing, blocked, done, total, active, progressPct }` per board.
   - **`UserClient.getUserSummaries(distinctMemberUserIds)`**: Mengambil inisial & avatar URL member.
3. **Penyusunan Meta**:
   - Agregasi `meta.summary` (total board aktif, hitung board dengan health `on_track` dan `at_risk`).
   - Agregasi `meta.groups` (group-by `board_group_id` / `group_name` beserta jumlah board aktif).

---

## 6. Rencana Verifikasi & Testing (BDD)

1. **Unit & Slice Tests**:
   - `BoardCardResponseMappingTest`: Memverifikasi perakitan metrik task, daysLeft, dan avatar.
   - `CreateBoardRequestValidationTest`: Uji YAVI validator terhadap regex board key dan rentang tanggal.
   - `BoardControllerTest`: Uji `standaloneSetup` MockMvc untuk seluruh endpoint Fase 1 (memverifikasi HTTP 200, 201, 400 validation, 409 stale version, 428 if-match required).
   - `BoardServiceTest`: Uji business logic mocking client dan repository.
2. **Quality Gates**:
   - `./gradlew test --tests "com.hyperdata.proms.module.work.board.*"`
   - `./gradlew test --tests "*ArchitectureTest*"` (ArchUnit enforcement)
   - `./gradlew spotlessApply && ./gradlew spotlessCheck`

