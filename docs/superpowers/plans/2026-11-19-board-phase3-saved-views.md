# Board Phase 3 (Custom Saved Views) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun fitur manajemen filter dan tampilan tersimpan (*Custom Saved Views*) pada modul `com.hyperdata.proms.module.work.board`, meliputi pengambilan daftar custom views (API-67) dan penyimpanan preset filter/sort baru (API-68).

**Architecture:** Memanfaatkan 2-Layer Modular Monolith Spring Boot dalam domain internal `work.board` tanpa cross-module dependency. Konfigurasi filter dan sorting fleksibel disimpan menggunakan PostgreSQL `JSONB` yang dipetakan melalui Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`. Memanfaatkan indeks parsial `board_saved_views_one_default` dengan mekanisme reset otomatis saat user menandai view baru sebagai default.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Data JPA, PostgreSQL 16 RLS, Hibernate 6 JSONB, YAVI Validation, MapStruct, JUnit 5, AssertJ, Mockito.

**Spec:** [docs/superpowers/specs/2026-11-19-board-phase3-saved-views-design.md](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-19-board-phase3-saved-views-design.md)

## Global Constraints

- Runtime: Java 21 (Temurin 21) & Spring Boot 4.1.x
- Root Package: `com.hyperdata.proms.module.work.board`
- UUIDv7 Mandatory: `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` pada entitas JPA `BoardSavedView`
- DTO Immutability: Semua request dan response DTO wajib menggunakan Java 21 `record`
- No Foreign Entity Leakage: Controller tidak boleh mengembalikan JPA `@Entity`
- PostgreSQL JSONB Mapping: Kolom `filters` dan `sort_config` wajib menggunakan `@JdbcTypeCode(SqlTypes.JSON)`
- Idempotency Mandatory: Pembuatan saved view baru (`POST`) wajib menyertakan header `Idempotency-Key`
- Git Policy: Jangan pernah menjalankan `git commit` atau `git push` secara otomatis

---

### Task 1: Entitas & Repository BoardSavedView (JSONB Mapping)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/constant/BoardViewType.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/entity/BoardSavedView.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/repository/BoardSavedViewRepository.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/repository/BoardSavedViewRepositoryTest.java`

**Interfaces:**
- Consumes: Skema tabel `proms.board_saved_views` (kolom JSONB `filters` dan `sort_config`)
- Produces: `BoardSavedViewRepository` untuk mengambil daftar view dan mereset status default view

- [ ] **Step 1: Tulis unit test untuk BoardSavedViewRepository**

```java
package com.hyperdata.proms.module.work.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardViewType;
import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import java.util.Map;
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
class BoardSavedViewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BoardSavedViewRepository savedViewRepository;

    @Test
    @DisplayName("Harus dapat menyimpan dan membaca BoardSavedView dengan konfigurasi JSONB")
    void shouldSaveAndRetrieveSavedViewWithJsonb() {
        UUID tenantId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BoardSavedView view = BoardSavedView.builder()
            .tenantId(tenantId)
            .boardId(boardId)
            .userId(userId)
            .name("High Priority Hardware")
            .viewType(BoardViewType.LIST)
            .filters(Map.of("priority", "high", "overdue", true))
            .sortConfig(Map.of("dueDate", "asc"))
            .isDefault(true)
            .position(1)
            .build();

        savedViewRepository.saveAndFlush(view);
        entityManager.clear();

        var list = savedViewRepository.findByTenantIdAndBoardIdAndUserId(tenantId, boardId, userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("High Priority Hardware");
        assertThat(list.get(0).getFilters()).containsEntry("priority", "high");
    }
}
```

- [ ] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.repository.BoardSavedViewRepositoryTest"`
Expected: FAIL (class `BoardSavedView` dan `BoardSavedViewRepository` belum ada).

- [ ] **Step 3: Implementasikan BoardViewType, BoardSavedView, dan BoardSavedViewRepository**

Buat `BoardViewType.java`:
```java
package com.hyperdata.proms.module.work.board.constant;

public final class BoardViewType {
    public static final String LIST = "list";
    public static final String BOARD = "board";
    public static final String TIMELINE = "timeline";
    public static final String CALENDAR = "calendar";

    private BoardViewType() {}
}
```

Buat `BoardSavedView.java`:
```java
package com.hyperdata.proms.module.work.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private String viewType;

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

Buat `BoardSavedViewRepository.java`:
```java
package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardSavedViewRepository extends JpaRepository<BoardSavedView, UUID> {

    @Query("""
        SELECT v FROM BoardSavedView v
        WHERE v.tenantId = :tenantId
          AND v.boardId = :boardId
          AND (v.userId = :userId OR v.isDefault = true)
        ORDER BY v.position ASC
    """)
    List<BoardSavedView> findByTenantIdAndBoardIdAndUserId(
        @Param("tenantId") UUID tenantId,
        @Param("boardId") UUID boardId,
        @Param("userId") UUID userId
    );

    @Query("""
        SELECT COALESCE(MAX(v.position), 0) FROM BoardSavedView v
        WHERE v.tenantId = :tenantId
          AND v.boardId = :boardId
          AND v.userId = :userId
    """)
    int findMaxPosition(
        @Param("tenantId") UUID tenantId,
        @Param("boardId") UUID boardId,
        @Param("userId") UUID userId
    );

    @Modifying
    @Query("""
        UPDATE BoardSavedView v
        SET v.isDefault = false
        WHERE v.tenantId = :tenantId
          AND v.boardId = :boardId
          AND v.userId = :userId
          AND v.isDefault = true
    """)
    void resetDefaultFlag(
        @Param("tenantId") UUID tenantId,
        @Param("boardId") UUID boardId,
        @Param("userId") UUID userId
    );
}
```

- [ ] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.repository.BoardSavedViewRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Verifikasi deliverable Task 1**

Run: `./gradlew spotlessApply`

---

### Task 2: DTOs & Validation untuk Custom Saved Views

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/CreateSavedViewRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/SavedViewResponse.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/CreateSavedViewRequestValidationTest.java`

**Interfaces:**
- Consumes: YAVI validation framework
- Produces: `CreateSavedViewRequest` dan `SavedViewResponse`

- [ ] **Step 1: Tulis unit test validasi CreateSavedViewRequest**

```java
package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardViewType;
import com.hyperdata.proms.module.work.board.dto.CreateSavedViewRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateSavedViewRequestValidationTest {

    @Test
    @DisplayName("Request valid harus lolos validasi YAVI")
    void validRequestShouldPass() {
        var req = new CreateSavedViewRequest(
            "Hardware Overdue",
            BoardViewType.LIST,
            Map.of("overdue", true),
            Map.of(),
            null,
            false
        );
        var violations = CreateSavedViewRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    @DisplayName("Nama view yang kosong harus gagal validasi")
    void emptyNameShouldFail() {
        var req = new CreateSavedViewRequest(
            "",
            BoardViewType.LIST,
            Map.of(),
            Map.of(),
            null,
            false
        );
        var violations = CreateSavedViewRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("name"));
    }
}
```

- [ ] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.CreateSavedViewRequestValidationTest"`
Expected: FAIL (class `CreateSavedViewRequest` belum ada).

- [ ] **Step 3: Implementasikan CreateSavedViewRequest dan SavedViewResponse**

Buat `CreateSavedViewRequest.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Payload penyimpanan custom filter view baru")
public record CreateSavedViewRequest(
    @Schema(description = "Nama view tersimpan", example = "Overdue hardware", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(description = "Tipe view (list, board, timeline, calendar)", example = "list", requiredMode = Schema.RequiredMode.REQUIRED)
    String type,
    @Schema(description = "Konfigurasi kriteria filter JSON")
    Map<String, Object> filters,
    @Schema(description = "Konfigurasi pengurutan JSON")
    Map<String, Object> sortConfig,
    @Schema(description = "Pengelompokan (group by)")
    String groupBy,
    @Schema(description = "Jadikan sebagai default view", example = "false")
    Boolean isDefault
) {
    public static final Validator<CreateSavedViewRequest> VALIDATOR = ValidatorBuilder.<CreateSavedViewRequest>of()
        .constraint(CreateSavedViewRequest::name, "name", c -> c.notBlank().lessThanOrEqual(100))
        .constraint(CreateSavedViewRequest::type, "type", c -> c.notBlank())
        .build();
}
```

Buat `SavedViewResponse.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Representasi saved view kustom pada board")
public record SavedViewResponse(
    UUID id,
    String name,
    String type,
    Map<String, Object> filters,
    Map<String, Object> sortConfig,
    String groupBy,
    boolean isDefault,
    int position,
    String version
) {}
```

- [ ] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.CreateSavedViewRequestValidationTest"`
Expected: PASS

- [ ] **Step 5: Verifikasi deliverable Task 2**

Run: `./gradlew spotlessApply`

---

### Task 3: Service Layer Manajemen Saved Views

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/service/BoardService.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/mapper/BoardMapper.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/BoardServiceTest.java`

**Interfaces:**
- Consumes: `BoardSavedViewRepository`
- Produces: Logika bisnis `getSavedViews` dan `createSavedView`

- [ ] **Step 1: Tulis unit test untuk saved view operations di BoardServiceTest**

```java
@Test
@DisplayName("createSavedView harus mereset default flag lama jika view baru ditandai isDefault")
void createSavedViewShouldResetOldDefaultFlag() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID boardId = UUID.randomUUID();
    CurrentUser user = new CurrentUser(userId, tenantId, "User", "user@hyperdata.co.id", List.of("MEMBER"));

    var req = new CreateSavedViewRequest(
        "Default Kanban", "board", Map.of(), Map.of(), null, true
    );

    when(boardRepository.existsByIdAndTenantId(boardId, tenantId)).thenReturn(true);
    when(savedViewRepository.findMaxPosition(tenantId, boardId, userId)).thenReturn(2);
    when(savedViewRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = boardService.createSavedView(user, boardId, req);

    verify(savedViewRepository).resetDefaultFlag(tenantId, boardId, userId);
    assertThat(response.isDefault()).isTrue();
    assertThat(response.position()).isEqualTo(3);
}
```

- [ ] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.BoardServiceTest"`
Expected: FAIL (method `createSavedView` belum ada di `BoardService`).

- [ ] **Step 3: Implementasikan logika bisnis di BoardService**

Tambahkan injeksi `BoardSavedViewRepository savedViewRepository;` dan implementasikan:
```java
@Transactional(readOnly = true)
public List<SavedViewResponse> getSavedViews(CurrentUser user, UUID boardId) {
    verifyBoardAccess(user.activeTenantId(), boardId, user.userId());
    List<BoardSavedView> views = savedViewRepository.findByTenantIdAndBoardIdAndUserId(
        user.activeTenantId(), boardId, user.userId()
    );
    return views.stream().map(boardMapper::toSavedViewResponse).toList();
}

@Transactional
public SavedViewResponse createSavedView(CurrentUser user, UUID boardId, CreateSavedViewRequest request) {
    CreateSavedViewRequest.VALIDATOR.validate(request).throwIfInvalid(ValidationException::new);
    verifyBoardAccess(user.activeTenantId(), boardId, user.userId());

    boolean isDefault = Boolean.TRUE.equals(request.isDefault());
    if (isDefault) {
        savedViewRepository.resetDefaultFlag(user.activeTenantId(), boardId, user.userId());
    }

    int nextPosition = savedViewRepository.findMaxPosition(user.activeTenantId(), boardId, user.userId()) + 1;

    BoardSavedView view = BoardSavedView.builder()
        .tenantId(user.activeTenantId())
        .boardId(boardId)
        .userId(user.userId())
        .name(request.name())
        .viewType(request.type())
        .filters(request.filters() != null ? request.filters() : Map.of())
        .sortConfig(request.sortConfig() != null ? request.sortConfig() : Map.of())
        .groupBy(request.groupBy())
        .isDefault(isDefault)
        .position(nextPosition)
        .build();

    BoardSavedView saved = savedViewRepository.saveAndFlush(view);
    return boardMapper.toSavedViewResponse(saved);
}
```

- [ ] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.BoardServiceTest"`
Expected: PASS

- [ ] **Step 5: Verifikasi deliverable Task 3**

Run: `./gradlew spotlessApply`

---

### Task 4: Controller Endpoints & OpenAPI Documentation (API-67 & API-68)

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/controller/BoardApi.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/controller/BoardController.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/controller/BoardControllerTest.java`

**Interfaces:**
- Consumes: `BoardService`
- Produces: REST Endpoints `GET /api/v1/boards/{boardId}/views` dan `POST /api/v1/boards/{boardId}/views`

- [ ] **Step 1: Tulis MockMvc controller tests untuk saved views endpoints**

Tambahkan pengujian di `BoardControllerTest.java`:
```java
@Test
@DisplayName("POST /api/v1/boards/{boardId}/views harus menghasilkan 201 Created")
void createSavedViewShouldReturn201() throws Exception {
    var req = new CreateSavedViewRequest("My Custom View", "list", Map.of(), Map.of(), null, false);
    when(boardService.createSavedView(any(), any(), any())).thenReturn(sampleSavedViewResponse());

    mockMvc.perform(post("/api/v1/boards/" + UUID.randomUUID() + "/views")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statusCode").value(201))
        .andExpect(jsonPath("$.data.name").value("My Custom View"));
}
```

- [ ] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.controller.BoardControllerTest"`
Expected: FAIL (endpoints `/views` belum terdaftar di `BoardApi`).

- [ ] **Step 3: Daftarkan endpoints di BoardApi dan delegasikan di BoardController**

Di `BoardApi.java`:
```java
@Operation(summary = "Daftar Saved Views Board", description = "Mengambil daftar preset filter view tersimpan pada board")
@GetMapping("/{boardId}/views")
RestResponse<List<SavedViewResponse>> getBoardSavedViews(@PathVariable("boardId") UUID boardId);

@Operation(summary = "Simpan Custom View Baru", description = "Menyimpan preset filter/sort baru sebagai view kustom")
@PostMapping("/{boardId}/views")
@ResponseStatus(HttpStatus.CREATED)
RestResponse<SavedViewResponse> createBoardSavedView(
    @PathVariable("boardId") UUID boardId,
    @RequestHeader("Idempotency-Key") UUID idempotencyKey,
    @RequestBody CreateSavedViewRequest request);
```

Implementasikan di `BoardController.java`.

- [ ] **Step 4: Jalankan controller test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.controller.BoardControllerTest"`
Expected: PASS

- [ ] **Step 5: Verifikasi deliverable Task 4**

Run: `./gradlew spotlessCheck`

---

### Task 5: Verifikasi Penuh Arsitektur & Quality Gates (DoD Verification)

**Files:**
- Test: Seluruh unit test suite modul `work.board`
- Test: ArchUnit architecture guardrails

- [ ] **Step 1: Jalankan targeted tests untuk modul work.board**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.*"`
Expected: BUILD SUCCESSFUL (Semua unit tests lulus).

- [ ] **Step 2: Jalankan architecture guardrail test**

Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
Expected: BUILD SUCCESSFUL (ArchUnit boundary rules lulus).

- [ ] **Step 3: Jalankan format code Spotless**

Run: `./gradlew spotlessApply && ./gradlew spotlessCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Jalankan full quality gate check**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL

