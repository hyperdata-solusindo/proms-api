# Core Board Module & Endpoint Integrasi (/boards/**) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun modul `com.hyperdata.proms.module.pm.board` lengkap dengan endpoint REST `/api/v1/boards/**` (API-12, API-13, API-23, API-14, API-19, API-44) serta integrasi *The Golden Stack* ke `UserFacade`, `TaskFacade`, dan `ChatFacade`.

**Architecture:** Menerapkan arsitektur 2-Layer Modular Monolith sesuai Master Directive `AGENTS.md`. `BoardController` murni bertindak sebagai thin controller yang mengimplementasikan `BoardApi`. Seluruh aturan bisnis, validasi izin, dan orkestrasi lintas modul berada di `BoardService` yang transaksional dengan antarmuka DTO Java 21 `record`.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Data JPA, MapStruct, YAVI, PostgreSQL 16 (Row-Level Security), JUnit 5, Mockito.

**Spec:** [`docs/superpowers/specs/2026-11-10-pm-board-core-design.md`](../specs/2026-11-10-pm-board-core-design.md)

## Global Constraints
- Java 21 (Eclipse Temurin 21)
- Spring Boot 4.1.x & Spring Data JPA
- 2-Layer Modular Monolith: `com.hyperdata.proms.module.<domain>.<feature>`
- Inter-Module Facade Rule: Hanya panggil interface `facade/*Facade.java` dan DTO di `facade/dto/*`. Dilarang mengimpor `Service` atau `Repository` asing.
- DTO Immutability: Semua DTO adalah Java 21 `record`. Dilarang membocorkan JPA `@Entity` ke Controller atau Facade.
- Entity Annotation: `@Getter`, `@Setter`, `@NoArgsConstructor`, dilarang menggunakan `@Data`.
- Constructor Injection murni: `@RequiredArgsConstructor` pada final fields (zero `@Autowired`).
- Validation: Menggunakan YAVI fluent validator (`Validator<T>`).
- Formatting & Quality Gate: Spotless dan ArchUnit wajib lolos (`./gradlew test --tests "*ArchitectureTest*"` dan `./gradlew spotlessCheck`).
- Git Policy: Jangan pernah menjalankan `git commit` atau `git push` otomatis.

---

### Task 1: Scaffolding Exception & DTOs Modul Board

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/constant/BoardCacheNames.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/dto/BoardSummaryResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/dto/BoardDetailResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/dto/CreateBoardTaskRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/facade/dto/BoardSummaryDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/facade/dto/BoardPermissionDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/facade/BoardFacade.java`
- Modify: `src/main/java/com/hyperdata/proms/common/exception/GlobalExceptionHandler.java` (atau custom exception handler)
- Test: `src/test/java/com/hyperdata/proms/module/pm/board/CreateBoardTaskRequestValidationTest.java`

**Interfaces:**
- Produces: `BoardFacade` interface, `BoardSummaryResponse`, `BoardDetailResponse`, `CreateBoardTaskRequest.VALIDATOR`.

- [ ] **Step 1: Tulis unit test validasi YAVI untuk CreateBoardTaskRequest**

Buat file `src/test/java/com/hyperdata/proms/module/pm/board/CreateBoardTaskRequestValidationTest.java`:
```java
package com.hyperdata.proms.module.pm.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateBoardTaskRequestValidationTest {

    @Test
    @DisplayName("given blank title when validate then returns violation")
    void givenBlankTitle_whenValidate_returnsViolation() {
        var req = new CreateBoardTaskRequest("", UUID.randomUUID(), null, List.of(), LocalDate.now(), "medium");
        var violations = CreateBoardTaskRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.get(0).message()).contains("Judul task wajib diisi");
    }

    @Test
    @DisplayName("given null workstreamId when validate then returns violation")
    void givenNullWorkstreamId_whenValidate_returnsViolation() {
        var req = new CreateBoardTaskRequest("Task 1", null, null, List.of(), LocalDate.now(), "medium");
        var violations = CreateBoardTaskRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.get(0).message()).contains("Workstream wajib dipilih");
    }

    @Test
    @DisplayName("given valid request when validate then returns success")
    void givenValidRequest_whenValidate_returnsSuccess() {
        var req = new CreateBoardTaskRequest("Task 1", UUID.randomUUID(), null, List.of(), LocalDate.now(), "high");
        var violations = CreateBoardTaskRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }
}
```

- [ ] **Step 2: Jalankan test untuk memastikan gagal (kompilasi merah)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.CreateBoardTaskRequestValidationTest"`
Expected: FAIL (Class not found)

- [ ] **Step 3: Implementasikan DTOs, Konstanta Cache, dan Exception**

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/constant/BoardCacheNames.java`:
```java
package com.hyperdata.proms.module.pm.board.constant;

public final class BoardCacheNames {
    private BoardCacheNames() {}
    public static final String BOARD_SUMMARY = "board:summary";
    public static final String BOARD_DETAIL = "board:detail";
    public static final String BOARD_PERMISSIONS = "board:permissions";
}
```

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/dto/CreateBoardTaskRequest.java`:
```java
package com.hyperdata.proms.module.pm.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBoardTaskRequest(
    String title,
    UUID workstreamId,
    UUID columnId,
    List<UUID> assigneeIds,
    LocalDate dueDate,
    String priority
) {
    public static final Validator<CreateBoardTaskRequest> VALIDATOR = ValidatorBuilder.<CreateBoardTaskRequest>of()
        .constraint(CreateBoardTaskRequest::title, "title", c -> c
            .notBlank().message("Judul task wajib diisi")
            .lessThanOrEqual(255).message("Judul task maksimal 255 karakter"))
        .constraint(CreateBoardTaskRequest::workstreamId, "workstreamId", c -> c
            .notNull().message("Workstream wajib dipilih"))
        .constraint(CreateBoardTaskRequest::priority, "priority", c -> c
            .predicate(p -> p == null || List.of("low", "medium", "high", "urgent").contains(p),
                "Priority tidak valid. Pilihan: low, medium, high, urgent"))
        .build();
}
```

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/dto/BoardSummaryResponse.java`:
```java
package com.hyperdata.proms.module.pm.board.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BoardSummaryResponse(
    UUID id,
    String key,
    String name,
    String status,
    String health,
    String healthReason,
    Integer progressPct,
    LocalDate dueDate,
    PmSummaryResponse pm,
    Integer memberCount,
    String myRole,
    Instant archivedAt,
    Instant version
) {
    public record PmSummaryResponse(UUID id, String fullName) {}
}
```

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/dto/BoardDetailResponse.java`:
```java
package com.hyperdata.proms.module.pm.board.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoardDetailResponse(
    UUID id,
    String key,
    String name,
    String status,
    List<WorkstreamResponse> workstreams,
    List<BoardColumnResponse> columns,
    List<BoardMemberResponse> members,
    BoardPermissionResponse permissions,
    Instant version
) {
    public record WorkstreamResponse(UUID id, String name, String color, Integer position) {}
    public record BoardColumnResponse(UUID id, String name, String category, Integer wipLimit, String position, Integer taskCount) {}
    public record BoardMemberResponse(UUID id, String fullName, String avatarUrl, String initials, String role, Boolean isOwner) {}
    public record BoardPermissionResponse(Boolean canManageStructure, Boolean canCreateTask, Boolean canArchiveBoard) {}
}
```

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/facade/dto/BoardSummaryDto.java`:
```java
package com.hyperdata.proms.module.pm.board.facade.dto;

import java.util.UUID;

public record BoardSummaryDto(UUID id, String key, String name, String status) {}
```

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/facade/dto/BoardPermissionDto.java`:
```java
package com.hyperdata.proms.module.pm.board.facade.dto;

import java.util.UUID;

public record BoardPermissionDto(
    UUID boardId,
    UUID userId,
    String role,
    boolean isOwner,
    String boardStatus
) {}
```

Buat file `src/main/java/com/hyperdata/proms/module/pm/board/facade/BoardFacade.java`:
```java
package com.hyperdata.proms.module.pm.board.facade;

import com.hyperdata.proms.module.pm.board.facade.dto.BoardPermissionDto;
import com.hyperdata.proms.module.pm.board.facade.dto.BoardSummaryDto;
import java.util.Optional;
import java.util.UUID;

public interface BoardFacade {
    boolean hasAccess(UUID boardId, UUID userId);
    Optional<BoardPermissionDto> getPermission(UUID boardId, UUID userId);
    void validateForTaskCreation(UUID boardId, UUID workstreamId, UUID columnId);
    Optional<BoardSummaryDto> findById(UUID boardId);
}
```

- [ ] **Step 4: Jalankan test validasi YAVI dan pastikan PASS**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.CreateBoardTaskRequestValidationTest"`
Expected: PASS

- [ ] **Step 5: Verifikasi Task 1**

Verifikasi bahwa semua record DTO terkompilasi bersih tanpa ada dependensi ke entity JPA.

---

### Task 2: Entitas JPA & Repositori Modul Board

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/entity/Board.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/entity/BoardMember.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/entity/BoardColumn.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/entity/Workstream.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/repository/BoardRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/repository/BoardMemberRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/repository/BoardColumnRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/repository/WorkstreamRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/repository/spec/BoardSpecs.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/mapper/BoardMapper.java`
- Test: `src/test/java/com/hyperdata/proms/module/pm/board/BoardRepositoryTest.java`

**Interfaces:**
- Produces: `BoardRepository`, `BoardMemberRepository`, `BoardColumnRepository`, `WorkstreamRepository`, `BoardSpecs`.

- [ ] **Step 1: Buat JPA Entities sesuai schema changelog PostgreSQL**

Buat `Board.java`:
```java
package com.hyperdata.proms.module.pm.board.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "boards", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class Board {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "pm_id")
    private UUID pmId;

    @Column(name = "status", nullable = false)
    private String status = "active";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "task_seq", nullable = false)
    private Integer taskSeq = 0;

    @Column(name = "health")
    private String health;

    @Column(name = "health_reason")
    private String healthReason;

    @Column(name = "progress_pct")
    private Integer progressPct;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
```

Buat `BoardMember.java`:
```java
package com.hyperdata.proms.module.pm.board.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_members", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class BoardMember {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "is_owner", nullable = false)
    private Boolean isOwner = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Buat `BoardColumn.java`:
```java
package com.hyperdata.proms.module.pm.board.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_columns", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class BoardColumn {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "workstream_id")
    private UUID workstreamId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "color")
    private String color;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
```

Buat `Workstream.java`:
```java
package com.hyperdata.proms.module.pm.board.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workstreams", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class Workstream {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color")
    private String color;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
```

- [ ] **Step 2: Buat Repositories & Dynamic Specifications**

Buat `BoardRepository.java`:
```java
package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.Board;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BoardRepository extends JpaRepository<Board, UUID>, JpaSpecificationExecutor<Board> {
    Optional<Board> findByIdAndDeletedAtIsNull(UUID id);
}
```

Buat `BoardMemberRepository.java`:
```java
package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.BoardMember;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {
    Optional<BoardMember> findByBoardIdAndUserId(UUID boardId, UUID userId);
    List<BoardMember> findByBoardId(UUID boardId);

    @Query("SELECT bm.boardId, COUNT(bm.id) FROM BoardMember bm WHERE bm.boardId IN :boardIds GROUP BY bm.boardId")
    List<Object[]> countMembersGroupedByBoardIds(@Param("boardIds") Set<UUID> boardIds);

    @Query("SELECT bm.boardId, bm.role FROM BoardMember bm WHERE bm.userId = :userId AND bm.boardId IN :boardIds")
    List<Object[]> findRolesByUserAndBoardIds(@Param("userId") UUID userId, @Param("boardIds") Set<UUID> boardIds);
}
```

Buat `BoardColumnRepository.java`:
```java
package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.BoardColumn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(UUID boardId);
    Optional<BoardColumn> findByIdAndBoardIdAndDeletedAtIsNull(UUID id, UUID boardId);
}
```

Buat `WorkstreamRepository.java`:
```java
package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.Workstream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkstreamRepository extends JpaRepository<Workstream, UUID> {
    List<Workstream> findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(UUID boardId);
    Optional<Workstream> findByIdAndBoardIdAndDeletedAtIsNull(UUID id, UUID boardId);
}
```

Buat `BoardSpecs.java`:
```java
package com.hyperdata.proms.module.pm.board.repository.spec;

import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardMember;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BoardSpecs {
    private BoardSpecs() {}

    public static Specification<Board> withFilters(String tab, String q, UUID userId) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates, cb.isNull(root.get("deletedAt")));

            // Filter tab
            Subquery<UUID> memberSubquery = query.subquery(UUID.class);
            var memberRoot = memberSubquery.from(BoardMember.class);
            memberSubquery.select(memberRoot.get("boardId"));

            if ("archived".equalsIgnoreCase(tab)) {
                predicates = cb.and(predicates, cb.isNotNull(root.get("archivedAt")));
                memberSubquery.where(cb.equal(memberRoot.get("userId"), userId));
                predicates = cb.and(predicates, root.get("id").in(memberSubquery));
            } else if ("shared".equalsIgnoreCase(tab)) {
                predicates = cb.and(predicates, cb.isNull(root.get("archivedAt")));
                predicates = cb.and(predicates, cb.equal(root.get("status"), "active"));
                memberSubquery.where(
                    cb.equal(memberRoot.get("userId"), userId),
                    cb.isFalse(memberRoot.get("isOwner")),
                    cb.notEqual(memberRoot.get("role"), "admin")
                );
                predicates = cb.and(predicates, root.get("id").in(memberSubquery));
            } else { // default "mine"
                predicates = cb.and(predicates, cb.isNull(root.get("archivedAt")));
                predicates = cb.and(predicates, cb.equal(root.get("status"), "active"));
                memberSubquery.where(cb.equal(memberRoot.get("userId"), userId));
                predicates = cb.and(predicates, root.get("id").in(memberSubquery));
            }

            // Filter keyword q
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                var nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                var keyMatch = cb.like(cb.lower(root.get("key")), pattern);
                predicates = cb.and(predicates, cb.or(nameMatch, keyMatch));
            }

            return predicates;
        };
    }
}
```

Buat `BoardMapper.java`:
```java
package com.hyperdata.proms.module.pm.board.mapper;

import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardColumn;
import com.hyperdata.proms.module.pm.board.entity.Workstream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BoardMapper {

    @Mapping(target = "id", source = "board.id")
    @Mapping(target = "version", source = "board.updatedAt")
    @Mapping(target = "pm", source = "pm")
    @Mapping(target = "memberCount", source = "memberCount")
    @Mapping(target = "myRole", source = "myRole")
    BoardSummaryResponse toSummaryResponse(Board board, BoardSummaryResponse.PmSummaryResponse pm, int memberCount, String myRole);

    BoardDetailResponse.BoardColumnResponse toColumnResponse(BoardColumn column);
    BoardDetailResponse.WorkstreamResponse toWorkstreamResponse(Workstream workstream);
}
```

- [ ] **Step 3: Jalankan ArchUnit check**

Run: `./gradlew test --tests "*ArchitectureTest*"`
Expected: PASS (Semua entitas berada di package entity dan tidak memakai `@Data`).

---

### Task 3: Kontrak Interface & Default Implementation Modul Fasad Luar (Task & Chat)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/pm/task/facade/TaskFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/task/facade/dto/BoardTaskFilterDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/task/facade/dto/TaskSummaryDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/task/facade/dto/CreateTaskCommand.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/task/facade/dto/TaskCreatedDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/task/facade/internal/DefaultTaskFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/communication/chat/facade/ChatFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/communication/chat/facade/dto/ChatChannelSummaryDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/communication/chat/facade/internal/DefaultChatFacade.java`
- Modify: `src/main/java/com/hyperdata/proms/module/identity/user/facade/UserFacade.java` & `DefaultUserFacade.java` (tambahkan method `findSummariesByIds(Set<UUID> ids)`)
- Test: `src/test/java/com/hyperdata/proms/module/pm/task/DefaultTaskFacadeTest.java`

- [ ] **Step 1: Deklarasikan DTOs & Kontrak `TaskFacade`**

Buat DTOs di `com.hyperdata.proms.module.pm.task.facade.dto`:
- `BoardTaskFilterDto.java`
- `TaskSummaryDto.java`
- `CreateTaskCommand.java`
- `TaskCreatedDto.java`

Buat interface `TaskFacade.java`:
```java
package com.hyperdata.proms.module.pm.task.facade;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import java.util.UUID;

public interface TaskFacade {
    PageResponse<TaskSummaryDto> getTasksByBoard(UUID boardId, BoardTaskFilterDto filter);
    TaskCreatedDto createTask(UUID boardId, CreateTaskCommand command);
}
```

Buat implementasi awal `DefaultTaskFacade.java`:
```java
package com.hyperdata.proms.module.pm.task.facade.internal;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.task.facade.TaskFacade;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultTaskFacade implements TaskFacade {

    @Override
    public PageResponse<TaskSummaryDto> getTasksByBoard(UUID boardId, BoardTaskFilterDto filter) {
        return PageResponse.of(List.of(), null);
    }

    @Override
    public TaskCreatedDto createTask(UUID boardId, CreateTaskCommand command) {
        return new TaskCreatedDto(UUID.randomUUID(), command.boardKey() + "-1", command.title(), command.columnId(), command.workstreamId(), Instant.now());
    }
}
```

- [ ] **Step 2: Deklarasikan DTOs & Kontrak `ChatFacade`**

Buat DTO `ChatChannelSummaryDto.java` di `com.hyperdata.proms.module.communication.chat.facade.dto`.

Buat interface `ChatFacade.java`:
```java
package com.hyperdata.proms.module.communication.chat.facade;

import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import java.util.List;
import java.util.UUID;

public interface ChatFacade {
    List<ChatChannelSummaryDto> getChannelsByBoard(UUID boardId);
}
```

Buat `DefaultChatFacade.java` di `internal/`:
```java
package com.hyperdata.proms.module.communication.chat.facade.internal;

import com.hyperdata.proms.module.communication.chat.facade.ChatFacade;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultChatFacade implements ChatFacade {

    @Override
    public List<ChatChannelSummaryDto> getChannelsByBoard(UUID boardId) {
        return List.of();
    }
}
```

- [ ] **Step 3: Tambahkan method `findSummariesByIds` pada `UserFacade` (Golden Stack Support)**

Perbarui `UserFacade.java`:
```java
List<UserSummaryDto> findSummariesByIds(Set<UUID> userIds);
```
Dan implementasikan di `DefaultUserFacade.java`:
```java
@Override
public List<UserSummaryDto> findSummariesByIds(Set<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
        return List.of();
    }
    return userRepository.findAllById(userIds).stream()
        .map(u -> new UserSummaryDto(u.getId(), u.getFullName(), u.getAvatarUrl(), u.getEmail()))
        .toList();
}
```

- [ ] **Step 4: Jalankan pengujian kompilasi & ArchUnit**

Run: `./gradlew test --tests "*ArchitectureTest*"`
Expected: PASS (Semua fasad baru lolos pengecekan `facades_must_be_interfaces` dan `facade_implementations_must_reside_in_internal_and_be_component`).

---

### Task 4: Implementasi BoardService & DefaultBoardFacade

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/service/BoardService.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/facade/internal/DefaultBoardFacade.java`
- Test: `src/test/java/com/hyperdata/proms/module/pm/board/BoardServiceTest.java`

**Interfaces:**
- Produces: `BoardService` method `listBoards`, `getBoardDetail`, `getBoardTasks`, `createBoardTask`, `getBoardChatChannels`.

- [ ] **Step 1: Tulis Unit Test untuk `BoardService`**

Buat `src/test/java/com/hyperdata/proms/module/pm/board/BoardServiceTest.java`:
```java
package com.hyperdata.proms.module.pm.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.module.communication.chat.facade.ChatFacade;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardMember;
import com.hyperdata.proms.module.pm.board.mapper.BoardMapper;
import com.hyperdata.proms.module.pm.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardRepository;
import com.hyperdata.proms.module.pm.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.pm.board.service.BoardService;
import com.hyperdata.proms.module.pm.task.facade.TaskFacade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardMemberRepository boardMemberRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private WorkstreamRepository workstreamRepository;
    @Mock private BoardMapper boardMapper;
    @Mock private UserFacade userFacade;
    @Mock private TaskFacade taskFacade;
    @Mock private ChatFacade chatFacade;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(
            boardRepository,
            boardMemberRepository,
            boardColumnRepository,
            workstreamRepository,
            boardMapper,
            userFacade,
            taskFacade,
            chatFacade
        );
    }

    @Test
    @DisplayName("given active user when listBoards then returns enriched boards")
    void givenActiveUser_whenListBoards_returnsEnrichedBoards() {
        var board = new Board();
        board.setId(UUID.randomUUID());
        board.setKey("NB");
        board.setName("Nusa Bio");
        board.setPmId(UUID.randomUUID());

        when(boardRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(board)));
        when(userFacade.findSummariesByIds(any())).thenReturn(List.of());

        var result = boardService.listBoards("mine", null, "updatedAt", 20, null);

        assertThat(result).isNotNull();
        verify(userFacade).findSummariesByIds(any());
    }

    @Test
    @DisplayName("given non-member user when getBoardDetail then throws 404")
    void givenNonMemberUser_whenGetBoardDetail_throwsNotFound() {
        UUID boardId = UUID.randomUUID();
        when(boardMemberRepository.findByBoardIdAndUserId(eq(boardId), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardDetail(boardId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Board tidak ditemukan");
    }
}
```

- [ ] **Step 2: Jalankan test untuk memastikan failing**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.BoardServiceTest"`
Expected: FAIL (`BoardService` not implemented yet).

- [ ] **Step 3: Implementasikan `BoardService` & `DefaultBoardFacade`**

Buat `src/main/java/com/hyperdata/proms/module/pm/board/service/BoardService.java`:
```java
package com.hyperdata.proms.module.pm.board.service;

import com.hyperdata.proms.common.exception.ForbiddenException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.security.SecurityContextUtils;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.module.communication.chat.facade.ChatFacade;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.identity.user.facade.dto.UserSummaryDto;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardColumn;
import com.hyperdata.proms.module.pm.board.entity.BoardMember;
import com.hyperdata.proms.module.pm.board.entity.Workstream;
import com.hyperdata.proms.module.pm.board.mapper.BoardMapper;
import com.hyperdata.proms.module.pm.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardRepository;
import com.hyperdata.proms.module.pm.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.pm.board.repository.spec.BoardSpecs;
import com.hyperdata.proms.module.pm.task.facade.TaskFacade;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final WorkstreamRepository workstreamRepository;
    private final BoardMapper boardMapper;

    private final UserFacade userFacade;
    private final TaskFacade taskFacade;
    private final ChatFacade chatFacade;

    public PageResponse<BoardSummaryResponse> listBoards(String tab, String q, String sort, int limit, String cursor) {
        UUID currentUserId = SecurityContextUtils.getCurrentUserId();
        var pageable = PageRequest.of(0, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "updatedAt"));
        var page = boardRepository.findAll(BoardSpecs.withFilters(tab, q, currentUserId), pageable);

        Set<UUID> pmIds = Lookups.toSet(page.getContent(), Board::getPmId);
        Map<UUID, UserSummaryDto> userMap = Lookups.toMap(userFacade.findSummariesByIds(pmIds), UserSummaryDto::id);

        Set<UUID> boardIds = Lookups.toSet(page.getContent(), Board::getId);
        Map<UUID, Integer> memberCountMap = new HashMap<>();
        for (Object[] row : boardMemberRepository.countMembersGroupedByBoardIds(boardIds)) {
            memberCountMap.put((UUID) row[0], ((Number) row[1]).intValue());
        }

        Map<UUID, String> myRoleMap = new HashMap<>();
        for (Object[] row : boardMemberRepository.findRolesByUserAndBoardIds(currentUserId, boardIds)) {
            myRoleMap.put((UUID) row[0], (String) row[1]);
        }

        List<BoardSummaryResponse> items = page.getContent().stream()
            .map(b -> {
                var pmDto = userMap.get(b.getPmId());
                var pmResponse = pmDto != null ? new BoardSummaryResponse.PmSummaryResponse(pmDto.id(), pmDto.fullName()) : null;
                return boardMapper.toSummaryResponse(
                    b,
                    pmResponse,
                    memberCountMap.getOrDefault(b.getId(), 0),
                    myRoleMap.getOrDefault(b.getId(), "member")
                );
            })
            .toList();

        return PageResponse.of(items, null);
    }

    public BoardDetailResponse getBoardDetail(UUID boardId) {
        UUID currentUserId = SecurityContextUtils.getCurrentUserId();
        BoardMember currentMember = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));

        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));

        List<BoardColumn> columns = boardColumnRepository.findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(boardId);
        List<Workstream> workstreams = workstreamRepository.findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(boardId);
        List<BoardMember> members = boardMemberRepository.findByBoardId(boardId);

        Set<UUID> memberUserIds = Lookups.toSet(members, BoardMember::getUserId);
        Map<UUID, UserSummaryDto> userMap = Lookups.toMap(userFacade.findSummariesByIds(memberUserIds), UserSummaryDto::id);

        List<BoardDetailResponse.BoardMemberResponse> memberResponses = members.stream()
            .map(m -> {
                var u = userMap.get(m.getUserId());
                String name = u != null ? u.fullName() : "User";
                String avatar = u != null ? u.avatarUrl() : null;
                String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
                return new BoardDetailResponse.BoardMemberResponse(m.getUserId(), name, avatar, initials, m.getRole(), m.getIsOwner());
            })
            .toList();

        boolean canManage = currentMember.getIsOwner() || List.of("admin", "pm").contains(currentMember.getRole());
        var permissions = new BoardDetailResponse.BoardPermissionResponse(canManage, canManage, currentMember.getIsOwner());

        return new BoardDetailResponse(
            board.getId(),
            board.getKey(),
            board.getName(),
            board.getStatus(),
            workstreams.stream().map(boardMapper::toWorkstreamResponse).toList(),
            columns.stream().map(c -> new BoardDetailResponse.BoardColumnResponse(c.getId(), c.getName(), c.getCategory(), c.getWipLimit(), c.getPosition(), 0)).toList(),
            memberResponses,
            permissions,
            board.getUpdatedAt()
        );
    }

    public PageResponse<TaskSummaryDto> getBoardTasks(UUID boardId, BoardTaskFilterDto filter) {
        UUID currentUserId = SecurityContextUtils.getCurrentUserId();
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));

        boolean guestOnly = "guest".equalsIgnoreCase(member.getRole()) || filter.sharedWithClientOnly();
        var effectiveFilter = new BoardTaskFilterDto(
            filter.workstreamId(),
            filter.assigneeId(),
            filter.searchKeyword(),
            guestOnly,
            filter.limit(),
            filter.cursor()
        );
        return taskFacade.getTasksByBoard(boardId, effectiveFilter);
    }

    @Transactional
    public TaskCreatedDto createBoardTask(UUID boardId, CreateBoardTaskRequest request) {
        UUID currentUserId = SecurityContextUtils.getCurrentUserId();
        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));

        if ("archived".equalsIgnoreCase(board.getStatus())) {
            throw new IllegalStateException("Board diarsipkan, tidak bisa diubah");
        }

        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));

        if (!member.getIsOwner() && !List.of("admin", "pm").contains(member.getRole())) {
            throw new ForbiddenException("Role Anda tidak diizinkan membuat task");
        }

        workstreamRepository.findByIdAndBoardIdAndDeletedAtIsNull(request.workstreamId(), boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Workstream tidak valid"));

        UUID targetColumnId = request.columnId();
        if (targetColumnId != null) {
            boardColumnRepository.findByIdAndBoardIdAndDeletedAtIsNull(targetColumnId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Kolom tidak valid"));
        } else {
            var defaultCol = boardColumnRepository.findByBoardIdAndDeletedAtIsNullOrderByPositionAsc(boardId).stream().findFirst();
            targetColumnId = defaultCol.map(BoardColumn::getId).orElse(null);
        }

        var cmd = new CreateTaskCommand(
            board.getKey(),
            request.workstreamId(),
            targetColumnId,
            request.title(),
            request.assigneeIds(),
            request.dueDate(),
            request.priority(),
            currentUserId
        );
        return taskFacade.createTask(boardId, cmd);
    }

    public List<ChatChannelSummaryDto> getBoardChatChannels(UUID boardId) {
        UUID currentUserId = SecurityContextUtils.getCurrentUserId();
        boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));

        return chatFacade.getChannelsByBoard(boardId);
    }
}
```

Buat `src/main/java/com/hyperdata/proms/module/pm/board/facade/internal/DefaultBoardFacade.java`:
```java
package com.hyperdata.proms.module.pm.board.facade.internal;

import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardMember;
import com.hyperdata.proms.module.pm.board.facade.BoardFacade;
import com.hyperdata.proms.module.pm.board.facade.dto.BoardPermissionDto;
import com.hyperdata.proms.module.pm.board.facade.dto.BoardSummaryDto;
import com.hyperdata.proms.module.pm.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardRepository;
import com.hyperdata.proms.module.pm.board.repository.WorkstreamRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultBoardFacade implements BoardFacade {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final WorkstreamRepository workstreamRepository;

    @Override
    public boolean hasAccess(UUID boardId, UUID userId) {
        return boardMemberRepository.findByBoardIdAndUserId(boardId, userId).isPresent();
    }

    @Override
    public Optional<BoardPermissionDto> getPermission(UUID boardId, UUID userId) {
        return boardMemberRepository.findByBoardIdAndUserId(boardId, userId).map(m -> {
            var board = boardRepository.findByIdAndDeletedAtIsNull(boardId).orElse(null);
            String status = board != null ? board.getStatus() : "active";
            return new BoardPermissionDto(boardId, userId, m.getRole(), m.getIsOwner(), status);
        });
    }

    @Override
    public void validateForTaskCreation(UUID boardId, UUID workstreamId, UUID columnId) {
        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board tidak ditemukan"));
        if ("archived".equalsIgnoreCase(board.getStatus())) {
            throw new IllegalStateException("Board diarsipkan, tidak bisa diubah");
        }
        workstreamRepository.findByIdAndBoardIdAndDeletedAtIsNull(workstreamId, boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Workstream tidak valid"));
        if (columnId != null) {
            boardColumnRepository.findByIdAndBoardIdAndDeletedAtIsNull(columnId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Kolom tidak valid"));
        }
    }

    @Override
    public Optional<BoardSummaryDto> findById(UUID boardId) {
        return boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .map(b -> new BoardSummaryDto(b.getId(), b.getKey(), b.getName(), b.getStatus()));
    }
}
```

- [ ] **Step 4: Jalankan unit test `BoardServiceTest`**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.BoardServiceTest"`
Expected: PASS

---

### Task 5: Implementasi REST Controller & Routing OpenAPI (`BoardApi` & `BoardController`)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/controller/BoardApi.java`
- Create: `src/main/java/com/hyperdata/proms/module/pm/board/controller/BoardController.java`
- Test: `src/test/java/com/hyperdata/proms/module/pm/board/BoardControllerTest.java`

**Interfaces:**
- Produces: REST Endpoints `/api/v1/boards`, `/api/v1/boards/{id}`, `/api/v1/boards/{id}/tasks`, `/api/v1/boards/{id}/chat/channels`.

- [ ] **Step 1: Tulis Controller WebMvc Test**

Buat `src/test/java/com/hyperdata/proms/module/pm/board/BoardControllerTest.java`:
```java
package com.hyperdata.proms.module.pm.board;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.board.controller.BoardController;
import com.hyperdata.proms.module.pm.board.service.BoardService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BoardService boardService;

    @Test
    @DisplayName("GET /api/v1/boards returns 200 with standard envelope")
    void getBoards_returnsEnvelope() throws Exception {
        when(boardService.listBoards(any(), any(), any(), eq(20), any()))
            .thenReturn(PageResponse.of(List.of(), null));

        mockMvc.perform(get("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Berhasil"));
    }
}
```

- [ ] **Step 2: Jalankan test untuk memverifikasi gagal (Controller belum ada)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.BoardControllerTest"`
Expected: FAIL

- [ ] **Step 3: Implementasikan `BoardApi` & `BoardController`**

Buat `src/main/java/com/hyperdata/proms/module/pm/board/controller/BoardApi.java`:
```java
package com.hyperdata.proms.module.pm.board.controller;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Project Management - Board", description = "Endpoint pengelolaan papan kerja, tugas, dan percakapan board")
@RequestMapping("/api/v1/boards")
public interface BoardApi {

    @GetMapping
    @Operation(summary = "Daftar board user (mine, shared, archived)")
    RestResponse<List<BoardSummaryResponse>> listBoards(
        @RequestParam(defaultValue = "mine") String tab,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "updatedAt") String sort,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String cursor
    );

    @GetMapping("/{boardId}")
    @Operation(summary = "Detail board beserta kolom, workstream, dan anggota")
    RestResponse<BoardDetailResponse> getBoardDetail(@PathVariable UUID boardId);

    @GetMapping("/{boardId}/tasks")
    @Operation(summary = "Daftar task Kanban pada board")
    RestResponse<List<TaskSummaryDto>> getBoardTasks(
        @PathVariable UUID boardId,
        @RequestParam(required = false) UUID workstreamId,
        @RequestParam(required = false) UUID assigneeId,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) String cursor
    );

    @PostMapping("/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Quick add task baru ke dalam board")
    RestResponse<TaskCreatedDto> createBoardTask(
        @PathVariable UUID boardId,
        @RequestBody CreateBoardTaskRequest request
    );

    @GetMapping("/{boardId}/chat/channels")
    @Operation(summary = "Daftar channel percakapan terikat board")
    RestResponse<List<ChatChannelSummaryDto>> getBoardChatChannels(@PathVariable UUID boardId);
}
```

Buat `src/main/java/com/hyperdata/proms/module/pm/board/controller/BoardController.java`:
```java
package com.hyperdata.proms.module.pm.board.controller;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.validation.YaviValidator;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.board.service.BoardService;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardController implements BoardApi {

    private final BoardService boardService;

    @Override
    public RestResponse<List<BoardSummaryResponse>> listBoards(String tab, String q, String sort, int limit, String cursor) {
        PageResponse<BoardSummaryResponse> page = boardService.listBoards(tab, q, sort, limit, cursor);
        return RestResponse.ok(page.data(), page.meta());
    }

    @Override
    public RestResponse<BoardDetailResponse> getBoardDetail(UUID boardId) {
        return RestResponse.ok(boardService.getBoardDetail(boardId));
    }

    @Override
    public RestResponse<List<TaskSummaryDto>> getBoardTasks(UUID boardId, UUID workstreamId, UUID assigneeId, String q, int limit, String cursor) {
        var filter = new BoardTaskFilterDto(workstreamId, assigneeId, q, false, limit, cursor);
        PageResponse<TaskSummaryDto> page = boardService.getBoardTasks(boardId, filter);
        return RestResponse.ok(page.data(), page.meta());
    }

    @Override
    public RestResponse<TaskCreatedDto> createBoardTask(UUID boardId, CreateBoardTaskRequest request) {
        YaviValidator.validate(CreateBoardTaskRequest.VALIDATOR, request);
        TaskCreatedDto result = boardService.createBoardTask(boardId, request);
        return RestResponse.created(result);
    }

    @Override
    public RestResponse<List<ChatChannelSummaryDto>> getBoardChatChannels(UUID boardId) {
        return RestResponse.ok(boardService.getBoardChatChannels(boardId));
    }
}
```

- [ ] **Step 4: Jalankan WebMvc test dan pastikan PASS**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.BoardControllerTest"`
Expected: PASS

---

### Task 6: Verifikasi Penuh (DoD Gates: ArchUnit, Spotless, Full Suite)

**Files:**
- Modify: `docs/superpowers/plans/2026-11-10-pm-board-core-design.md` (Update checklist status)

- [ ] **Step 1: Jalankan ArchUnit Architecture Test**

Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
Expected: BUILD SUCCESS (Lolos semua aturan controller interface, facade interface, entity data check).

- [ ] **Step 2: Jalankan Unit Test Modul PM Board secara menyeluruh**

Run: `./gradlew test --tests "com.hyperdata.proms.module.pm.board.*"`
Expected: BUILD SUCCESS (Seluruh unit test controller, service, dan YAVI lolos).

- [ ] **Step 3: Format kode dengan Spotless**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Jalankan Spotless Check**

Run: `./gradlew spotlessCheck`
Expected: BUILD SUCCESS.
