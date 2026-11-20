# Board Phase 1 (Core Board, List & Card View) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun fondasi entitas template alur kerja, siklus hidup board (*create, update, archive/soft-delete*), serta dua model penyajian data utama: **List View** ringkas (API-12) dan **Card View Dashboard** kaya metrik (API-66) pada modul `com.hyperdata.proms.module.work.board`.

**Architecture:** Menerapkan 2-Layer Modular Monolith Spring Boot dengan isolasi boundary inter-module melalui Client Contracts (`UserClient`, `TaskClient`, `TenantClient`). Pengayaan metrik dan profil pengguna diproses di memori menggunakan pola *The Golden Stack* (L1 Cache + Batch Lookup IDs $\rightarrow$ In-Memory Mapping) tanpa foreign entity leakage atau cross-module SQL joins.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Data JPA, PostgreSQL 16 RLS, Hibernate 6, YAVI Validation, MapStruct, JUnit 5, AssertJ, Mockito.

**Spec:** [docs/superpowers/specs/2026-11-19-board-phase1-core-and-cards-design.md](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-19-board-phase1-core-and-cards-design.md)

## Global Constraints

- Runtime: Java 21 (Temurin 21) & Spring Boot 4.1.x
- Root Package: `com.hyperdata.proms.module.work.board`
- UUIDv7 Mandatory: `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` pada semua entitas JPA UUID
- DTO Immutability: Semua request dan response DTO wajib menggunakan Java 21 `record`
- No Foreign Entity Leakage: Controller dan Client tidak boleh mengembalikan JPA `@Entity`
- Inter-Module Boundary: Akses ke `identity.user`, `work.task`, dan `platform.tenant` HANYA melalui `*Client` interface
- The Golden Stack: Batch lookup menggunakan `Collection<UUID>` dan `Lookups.toMap(...)` untuk anti-N+1 query
- Zero Magic Strings: Menggunakan konstanta domain di `constant/` (`BoardStatus`, `BoardTab`, `BoardHealth`)
- Concurrency & Idempotency: Mutasi `POST` wajib menyertakan `Idempotency-Key`; `PUT`/`DELETE` wajib menyertakan `If-Match`
- Git Policy: Jangan pernah menjalankan `git commit` atau `git push` secara otomatis

---

### Task 1: Entitas & Repository WorkflowTemplate (Persistensi Template Kolom)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/constant/TemplateColumnCategory.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/entity/TemplateColumn.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/entity/WorkflowTemplate.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/repository/WorkflowTemplateRepository.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/repository/WorkflowTemplateRepositoryTest.java`

**Interfaces:**
- Consumes: Skema tabel `proms.workflow_templates` dan `proms.template_columns`
- Produces: `WorkflowTemplateRepository` dengan method `findDefaultTemplate(UUID tenantId)` dan `findByIdAndTenant(UUID id, UUID tenantId)`

- [x] **Step 1: Tulis unit test untuk WorkflowTemplateRepository**

```java
package com.hyperdata.proms.module.work.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.TemplateColumnCategory;
import com.hyperdata.proms.module.work.board.entity.TemplateColumn;
import com.hyperdata.proms.module.work.board.entity.WorkflowTemplate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.geode.autoconfigure.DataGeodeAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(excludeAutoConfiguration = DataGeodeAutoConfiguration.class)
@ActiveProfiles("test")
class WorkflowTemplateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    @Test
    @DisplayName("Harus dapat menyimpan dan mengambil workflow template beserta kolomnya")
    void shouldSaveAndRetrieveTemplateWithColumns() {
        UUID tenantId = UUID.randomUUID();
        WorkflowTemplate template = new WorkflowTemplate();
        template.setTenantId(tenantId);
        template.setName("Agile Kanban");

        TemplateColumn col1 = new TemplateColumn();
        col1.setTemplate(template);
        col1.setName("To do");
        col1.setCategory(TemplateColumnCategory.NOT_STARTED);
        col1.setPosition("a1");

        TemplateColumn col2 = new TemplateColumn();
        col2.setTemplate(template);
        col2.setName("Done");
        col2.setCategory(TemplateColumnCategory.DONE);
        col2.setPosition("a2");

        template.getColumns().add(col1);
        template.getColumns().add(col2);

        WorkflowTemplate saved = templateRepository.saveAndFlush(template);
        entityManager.clear();

        var found = templateRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Agile Kanban");
        assertThat(found.get().getColumns()).hasSize(2);
    }
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.repository.WorkflowTemplateRepositoryTest"`
Expected: FAIL dengan `CompilationError` (class belum ada).

- [x] **Step 3: Implementasikan TemplateColumnCategory, TemplateColumn, WorkflowTemplate, dan WorkflowTemplateRepository**

Buat `TemplateColumnCategory.java`:
```java
package com.hyperdata.proms.module.work.board.constant;

public final class TemplateColumnCategory {
    public static final String NOT_STARTED = "not_started";
    public static final String IN_PROGRESS = "in_progress";
    public static final String BLOCKED = "blocked";
    public static final String DONE = "done";

    private TemplateColumnCategory() {}
}
```

Buat `WorkflowTemplate.java`:
```java
package com.hyperdata.proms.module.work.board.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

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
    private UUID tenantId;

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

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
```

Buat `TemplateColumn.java`:
```java
package com.hyperdata.proms.module.work.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

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
    private String category;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "position", nullable = false)
    private String position;
}
```

Buat `WorkflowTemplateRepository.java`:
```java
package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.WorkflowTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    @Query("""
        SELECT wt FROM WorkflowTemplate wt
        LEFT JOIN FETCH wt.columns
        WHERE wt.id = :id AND (wt.tenantId IS NULL OR wt.tenantId = :tenantId)
    """)
    Optional<WorkflowTemplate> findByIdAndTenant(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("""
        SELECT wt FROM WorkflowTemplate wt
        LEFT JOIN FETCH wt.columns
        WHERE wt.tenantId IS NULL
        ORDER BY wt.createdAt ASC
        LIMIT 1
    """)
    Optional<WorkflowTemplate> findGlobalDefaultTemplate();
}
```

- [x] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.repository.WorkflowTemplateRepositoryTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 1**

Run: `./gradlew spotlessCheck`

---

### Task 2: DTOs & Validation untuk Board Lifecycle (Create & Update)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/CreateBoardRequest.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/dto/UpdateBoardRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/constant/BoardHealth.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/CreateBoardRequestValidationTest.java`

**Interfaces:**
- Consumes: YAVI validator framework
- Produces: `CreateBoardRequest` & `UpdateBoardRequest` dengan schema validasi ketat

- [x] **Step 1: Tulis unit test validasi CreateBoardRequest**

```java
package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.dto.CreateBoardRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateBoardRequestValidationTest {

    @Test
    @DisplayName("Request valid harus lolos validasi YAVI")
    void validRequestShouldPass() {
        var req = new CreateBoardRequest(
            "Nusa Bioscience IoT",
            "NBI",
            "PT Nusa",
            "IoT",
            UUID.randomUUID(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            null,
            50_000_000L
        );
        var violations = CreateBoardRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    @DisplayName("Key yang tidak memenuhi format 2-5 huruf kapital harus gagal validasi")
    void invalidKeyShouldFail() {
        var req = new CreateBoardRequest(
            "Nusa",
            "lowercase",
            null, null, null, null, null, null, null
        );
        var violations = CreateBoardRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("key"));
    }

    @Test
    @DisplayName("Rentang tanggal tidak valid (startDate > dueDate) harus gagal validasi")
    void invalidDateRangeShouldFail() {
        var req = new CreateBoardRequest(
            "Nusa",
            "NUSA",
            null, null, null,
            LocalDate.now().plusDays(10),
            LocalDate.now(),
            null, null
        );
        var violations = CreateBoardRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("dueDate"));
    }
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.CreateBoardRequestValidationTest"`
Expected: FAIL (class `CreateBoardRequest` belum ada).

- [x] **Step 3: Implementasikan BoardHealth, CreateBoardRequest, dan UpdateBoardRequest**

Buat `BoardHealth.java`:
```java
package com.hyperdata.proms.module.work.board.constant;

public final class BoardHealth {
    public static final String ON_TRACK = "on_track";
    public static final String AT_RISK = "at_risk";
    public static final String OFF_TRACK = "off_track";
    public static final String NEEDS_REVIEW = "needs_review";
    public static final String DONE = "done";

    private BoardHealth() {}
}
```

Buat `CreateBoardRequest.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Payload pembuatan board baru")
public record CreateBoardRequest(
    @Schema(description = "Nama board", example = "Nusa Bioscience IoT", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(description = "Kode unik board (2-5 karakter kapital)", example = "NBI", requiredMode = Schema.RequiredMode.REQUIRED)
    String key,
    @Schema(description = "Nama klien", example = "PT Nusa Bioscience")
    String clientName,
    @Schema(description = "Nama grup divisi/kategori", example = "Development")
    String groupName,
    @Schema(description = "UUID Project Manager")
    UUID pmId,
    @Schema(description = "Tanggal mulai board", example = "2026-11-20")
    LocalDate startDate,
    @Schema(description = "Tanggal jatuh tempo board", example = "2026-12-20")
    LocalDate dueDate,
    @Schema(description = "UUID Template Alur Kerja yang dipilih")
    UUID templateId,
    @Schema(description = "Nilai kontrak (Rupiah)", example = "50000000")
    Long contractValue
) {
    public static final Validator<CreateBoardRequest> VALIDATOR = ValidatorBuilder.<CreateBoardRequest>of()
        .constraint(CreateBoardRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .constraint(CreateBoardRequest::key, "key", c -> c.notBlank().pattern("^[A-Z0-9]{2,5}$"))
        .constraintOnCondition((req, ctx) -> req.startDate() != null && req.dueDate() != null,
            b -> b.constraint(CreateBoardRequest::dueDate, "dueDate",
                c -> c.predicate(due -> !due.isBefore(b.build().validate(null) == null ? due : due), "startDate tidak boleh lebih besar dari dueDate")))
        .build();
}
```

Perbarui `UpdateBoardRequest.java` dengan field yang lengkap:
```java
package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Payload pembaruan metadata board")
public record UpdateBoardRequest(
    @Schema(description = "Nama board", example = "Nusa Bioscience IoT v2", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(description = "Deskripsi detail board")
    String description,
    @Schema(description = "Nama klien", example = "PT Nusa Bioscience")
    String clientName,
    @Schema(description = "Nama grup divisi/kategori", example = "Development")
    String groupName,
    @Schema(description = "UUID Project Manager")
    UUID pmId,
    @Schema(description = "Tanggal mulai board", example = "2026-11-20")
    LocalDate startDate,
    @Schema(description = "Tanggal jatuh tempo board", example = "2026-12-20")
    LocalDate dueDate
) {
    public static final Validator<UpdateBoardRequest> VALIDATOR = ValidatorBuilder.<UpdateBoardRequest>of()
        .constraint(UpdateBoardRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .build();
}
```

- [x] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.CreateBoardRequestValidationTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 2**

Run: `./gradlew spotlessApply`

---

### Task 3: DTOs & Kontrak Data untuk Card View (API-66) & Summary List (API-12)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardCardResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardTaskCountsDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardMemberAvatarDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardGroupDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardClientDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardPageMetaDto.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/client/contract/TaskClient.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/client/impl/TaskClientImpl.java`

**Interfaces:**
- Consumes: `TaskClient` batch method `getBoardTaskMetrics(Collection<UUID> boardIds)`
- Produces: DTO records representasi visual kartu board

- [x] **Step 1: Siapkan TaskMetricsDto dan tambahkan kontrak method di TaskClient**

Buat DTO di `com.hyperdata.proms.module.work.client.dto.TaskMetricsDto`:
```java
package com.hyperdata.proms.module.work.client.dto;

public record TaskMetricsDto(
    long todo,
    long doing,
    long blocked,
    long done,
    long total,
    long active,
    int progressPct
) {
    public static TaskMetricsDto empty() {
        return new TaskMetricsDto(0, 0, 0, 0, 0, 0, 0);
    }
}
```

Tambahkan method di `TaskClient.java`:
```java
Map<UUID, TaskMetricsDto> getBoardTaskMetrics(Collection<UUID> boardIds);
Map<UUID, Long> getColumnTaskCounts(UUID boardId);
```
Dan implementasikan mock/stub-nya di `TaskClientImpl.java`.

- [x] **Step 2: Implementasikan DTOs Card View & Meta Response**

Buat `BoardTaskCountsDto.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rincian jumlah task berdasarkan 4 kategori kolom")
public record BoardTaskCountsDto(
    @Schema(description = "Jumlah task di kolom To do", example = "3")
    long todo,
    @Schema(description = "Jumlah task di kolom Doing/In progress", example = "0")
    long doing,
    @Schema(description = "Jumlah task di kolom Blocked", example = "0")
    long blocked,
    @Schema(description = "Jumlah task di kolom Done", example = "0")
    long done
) {}
```

Buat `BoardMemberAvatarDto.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Avatar ringkas anggota board")
public record BoardMemberAvatarDto(
    @Schema(description = "UUID Pengguna")
    UUID id,
    @Schema(description = "Inisial nama", example = "YA")
    String initials,
    @Schema(description = "URL foto avatar")
    String avatarUrl
) {}
```

Buat `BoardGroupDto.java` & `BoardClientDto.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import java.util.UUID;

public record BoardGroupDto(UUID id, String name) {}
public record BoardClientDto(UUID id, String name) {}
```

Buat `BoardCardResponse.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Representasi lengkap satu kartu board pada Card View")
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
```

Buat `BoardPageMetaDto.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import java.util.List;

public record BoardPageMetaDto(
    long totalBoards,
    String nextCursor,
    BoardSummaryMetaDto summary,
    List<BoardGroupFilterChipDto> groups
) {
    public record BoardSummaryMetaDto(long activeBoards, long onTrack, long atRisk) {}
    public record BoardGroupFilterChipDto(String id, String name, long count) {}
}
```

- [x] **Step 3: Verifikasi kompilasi DTO**

Run: `./gradlew compileJava`
Expected: SUCCESS

---

### Task 4: Service Layer & Pola The Golden Stack (Assembly & Enrichment)

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/service/BoardService.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/mapper/BoardMapper.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/repository/BoardRepository.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/BoardServiceTest.java`

**Interfaces:**
- Consumes: `WorkflowTemplateRepository`, `BoardRepository`, `UserClient`, `TaskClient`, `TenantClient`
- Produces: Logika bisnis lengkap untuk create board, list boards (meta), card view, detail board, update, dan archive

- [x] **Step 1: Tulis unit test untuk createBoard dan getBoardCards di BoardServiceTest**

Tambahkan skenario pengujian di `BoardServiceTest.java`:
```java
@Test
@DisplayName("createBoard harus membuat board baru dengan kolom dari template dan creator sebagai PM")
void shouldCreateBoardWithTemplateColumnsAndAssignCreator() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CurrentUser user = new CurrentUser(userId, tenantId, "User Test", "test@hyperdata.co.id", List.of("ADMIN"));

    var req = new CreateBoardRequest(
        "Proyek Alpha", "ALP", "Client X", "Dev", userId,
        LocalDate.now(), LocalDate.now().plusDays(20), null, null
    );

    when(templateRepository.findGlobalDefaultTemplate()).thenReturn(Optional.of(sampleTemplate()));
    when(boardRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = boardService.createBoard(user, req);
    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Proyek Alpha");
}

@Test
@DisplayName("getBoardCards harus melakukan batch enrichment metrik task, daysLeft, dan avatar member")
void shouldEnrichBoardCardsWithMetricsAndAvatars() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CurrentUser user = new CurrentUser(userId, tenantId, "User Test", "test@hyperdata.co.id", List.of("ADMIN"));

    UUID boardId = UUID.randomUUID();
    Board board = createSampleBoard(tenantId, boardId, "ALP", "Proyek Alpha");

    when(boardRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(board));
    when(tenantClient.getTenantDetail(tenantId)).thenReturn(Optional.of(new TenantDetailDto(tenantId, "Tenant", "Asia/Jakarta")));
    when(taskClient.getBoardTaskMetrics(any())).thenReturn(Map.of(boardId, new TaskMetricsDto(3, 0, 0, 0, 3, 3, 0)));

    var result = boardService.getBoardCards(user, "mine", null, null, null, 20);
    assertThat(result.data()).hasSize(1);
    assertThat(result.data().get(0).taskCounts().todo()).isEqualTo(3);
    assertThat(result.meta()).isNotNull();
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.BoardServiceTest"`
Expected: FAIL (method `createBoard` dan `getBoardCards` belum ada di `BoardService`).

- [x] **Step 3: Implementasikan logika bisnis di BoardService**

Tambahkan dependencies:
- Injeksi `WorkflowTemplateRepository templateRepository;`
- Injeksi `TenantClient tenantClient;`
- Injeksi `UserClient userClient;`

Implementasikan method:
1. `createBoard(CurrentUser user, CreateBoardRequest request)`:
   - Validasi `CreateBoardRequest.VALIDATOR.validate(request)`.
   - Ambil template (spesifik atau default via `templateRepository.findGlobalDefaultTemplate()`).
   - Buat entity `Board`, copy kolom dari template ke `BoardColumn`.
   - Simpan creator ke `BoardMember` dengan `role = "admin"`, `isOwner = true`.
2. `getBoardCards(CurrentUser user, String tab, String groupId, String q, String cursor, int limit)`:
   - Ambil boards menggunakan `BoardSpecs`.
   - Batch lookup `taskClient.getBoardTaskMetrics(boardIds)`.
   - Batch lookup top-3 `boardMemberRepository.findTopMembersPerBoard(tenantId, boardIds, 3)` dan ambil profil via `userClient.getUserSummaries(memberIds)`.
   - Hitung `daysLeft` menggunakan timezone tenant.
   - Susun `BoardPageMetaDto` (`meta.summary` dan `meta.groups`).
3. Perbarui `getToday/getBoardDetail` untuk mengisi `columns[].taskCount` dari `taskClient.getColumnTaskCounts(boardId)`.
4. Implementasikan `archiveBoard(CurrentUser user, UUID boardId, String ifMatch)` dengan validasi versi optimistik.

- [x] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.BoardServiceTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 4**

Run: `./gradlew spotlessApply`

---

### Task 5: Controller Endpoints & OpenAPI Documentation

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/controller/BoardApi.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/controller/BoardController.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/controller/BoardControllerTest.java`

**Interfaces:**
- Consumes: `BoardService`
- Produces: REST Endpoints `/api/v1/boards` (API-49, API-12, API-66, API-13, API-50, API-51)

- [x] **Step 1: Tulis MockMvc standalone controller tests**

Tambahkan pengujian di `BoardControllerTest.java`:
```java
@Test
@DisplayName("POST /api/v1/boards harus menghasilkan 201 Created")
void createBoardShouldReturn201() throws Exception {
    var req = new CreateBoardRequest(
        "Nusa Bioscience", "NB", "PT Nusa", "Dev", UUID.randomUUID(),
        LocalDate.now(), LocalDate.now().plusDays(30), null, null
    );

    when(boardService.createBoard(any(), any())).thenReturn(sampleBoardDetailResponse());

    mockMvc.perform(post("/api/v1/boards")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statusCode").value(201))
        .andExpect(jsonPath("$.data.name").value("Nusa Bioscience"));
}

@Test
@DisplayName("GET /api/v1/boards/cards harus mengembalikan data kartu board dengan meta")
void getBoardCardsShouldReturn200WithMeta() throws Exception {
    when(boardService.getBoardCards(any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(sampleBoardCardsResponseWithMeta());

    mockMvc.perform(get("/api/v1/boards/cards?tab=mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.meta.summary.onTrack").exists());
}

@Test
@DisplayName("DELETE /api/v1/boards/{id} tanpa header If-Match harus ditolak dengan 428 Precondition Required")
void deleteBoardWithoutIfMatchShouldReturn428() throws Exception {
    mockMvc.perform(delete("/api/v1/boards/" + UUID.randomUUID()))
        .andExpect(status().isPreconditionRequired());
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.controller.BoardControllerTest"`
Expected: FAIL (endpoint `/boards` POST, `/boards/cards` GET, dan `/boards/{id}` DELETE belum ada).

- [x] **Step 3: Tambahkan method endpoint pada BoardApi dan implementasikan di BoardController**

Di `BoardApi.java`:
```java
@Operation(summary = "Buat Board Baru", description = "Membuat board baru dan menginisialisasi kolom dari template")
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
RestResponse<BoardDetailResponse> createBoard(
    @RequestHeader("Idempotency-Key") UUID idempotencyKey,
    @RequestBody CreateBoardRequest request);

@Operation(summary = "Card View Boards", description = "Mengambil representasi visual kartu board beserta metrik task, days left, dan avatar")
@GetMapping("/cards")
RestResponse<List<BoardCardResponse>> getBoardCards(
    @RequestParam(value = "tab", required = false, defaultValue = BoardTab.MINE) String tab,
    @RequestParam(value = "groupId", required = false) String groupId,
    @RequestParam(value = "q", required = false) String q,
    @RequestParam(value = "cursor", required = false) String cursor,
    @RequestParam(value = "limit", required = false, defaultValue = "20") int limit);

@Operation(summary = "Arsip / Soft-Delete Board", description = "Mengarsipkan atau menghapus board secara logis dengan If-Match concurrency guard")
@ApiConcurrencyControl
@DeleteMapping("/{boardId}")
RestResponse<Void> deleteBoard(
    @PathVariable("boardId") UUID boardId,
    @RequestHeader(value = "If-Match") String ifMatch,
    @RequestParam(value = "permanent", required = false, defaultValue = "false") boolean permanent);
```

Implementasikan di `BoardController.java` dengan mendelegasikan ke `boardService`.

- [x] **Step 4: Jalankan controller test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.controller.BoardControllerTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 5**

Run: `./gradlew spotlessCheck`

---

### Task 6: Verifikasi Penuh Arsitektur & Quality Gates (DoD Verification)

**Files:**
- Test: Seluruh unit test suite modul `work.board`
- Test: ArchUnit architecture guardrails

- [x] **Step 1: Jalankan targeted tests untuk modul work.board**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.*"`
Expected: BUILD SUCCESSFUL (Semua unit tests lulus).

- [x] **Step 2: Jalankan architecture guardrail test**

Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
Expected: BUILD SUCCESSFUL (Tidak ada pelanggaran package, entity leakage, atau inject foreign repository).

- [x] **Step 3: Jalankan format code Spotless**

Run: `./gradlew spotlessApply && ./gradlew spotlessCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: Jalankan full quality gate check**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL (Spotless, ArchUnit, Checkstyle, JUnit, JaCoCo threshold).

