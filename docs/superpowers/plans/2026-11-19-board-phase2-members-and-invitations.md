# Board Phase 2 (Member Collaboration & Invitations) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun fitur kolaborasi anggota board pada modul `com.hyperdata.proms.module.work.board`, meliputi pengambilan daftar member & pending invitation (API-23), pengiriman undangan member baru (API-52), pembaruan role/workstream (API-53), dan penghapusan member dengan pembebasan task assignees sesuai aturan BR-DL-04 (API-54).

**Architecture:** Memanfaatkan 2-Layer Modular Monolith Spring Boot. Komunikasi lintas modul untuk mengecek status email user dan profil member dialirkan melalui `UserClient` (`identity.user`). Ketika member dikeluarkan dari board, `TaskClient` (`work.task`) dipanggil untuk melepaskan penugasan task (*unassign*) secara atomik. Menggunakan optimistik concurrency guard (`If-Match`) dan idempotency guard (`Idempotency-Key`).

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Data JPA, PostgreSQL 16 RLS, Hibernate 6, YAVI Validation, MapStruct, JUnit 5, AssertJ, Mockito.

**Spec:** [docs/superpowers/specs/2026-11-19-board-phase2-members-and-invitations-design.md](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-19-board-phase2-members-and-invitations-design.md)

## Global Constraints

- Runtime: Java 21 (Temurin 21) & Spring Boot 4.1.x
- Root Package: `com.hyperdata.proms.module.work.board`
- UUIDv7 Mandatory: `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` pada semua entitas JPA UUID
- DTO Immutability: Semua request dan response DTO wajib menggunakan Java 21 `record`
- No Foreign Entity Leakage: Controller dan Client tidak boleh mengembalikan JPA `@Entity`
- Inter-Module Boundary: Akses ke `identity.user` dan `work.task` HANYA melalui `*Client` interface
- The Golden Stack: Batch lookup menggunakan `Collection<UUID>` dan `Lookups.toMap(...)` untuk anti-N+1 query
- Concurrency & Idempotency: Mutasi `POST /invitations` wajib menyertakan `Idempotency-Key`; `PATCH`/`DELETE` member wajib menyertakan `If-Match`
- Business Rule BR-DL-04: Saat member dihapus dari board, semua task milik member di board tersebut wajib di-unassign
- Git Policy: Jangan pernah menjalankan `git commit` atau `git push` secara otomatis

---

### Task 1: Entitas & Repository BoardInvitation

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/constant/BoardInvitationResult.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/entity/BoardInvitation.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/repository/BoardInvitationRepository.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/repository/BoardInvitationRepositoryTest.java`

**Interfaces:**
- Consumes: Skema tabel `proms.board_invitations`
- Produces: `BoardInvitationRepository` untuk query invitation aktif/pending per board

- [x] **Step 1: Tulis unit test untuk BoardInvitationRepository**

```java
package com.hyperdata.proms.module.work.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.entity.BoardInvitation;
import java.time.Instant;
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
class BoardInvitationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BoardInvitationRepository invitationRepository;

    @Test
    @DisplayName("Harus dapat mengambil daftar invitation yang masih pending untuk sebuah board")
    void shouldFindPendingInvitationsByBoardId() {
        UUID tenantId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();

        BoardInvitation inv1 = BoardInvitation.builder()
            .tenantId(tenantId)
            .boardId(boardId)
            .email("budi@contoh.id")
            .boardRole(BoardMemberRole.MEMBER)
            .tokenHash("hash123")
            .invitedBy(invitedBy)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        BoardInvitation invExpired = BoardInvitation.builder()
            .tenantId(tenantId)
            .boardId(boardId)
            .email("lama@contoh.id")
            .boardRole(BoardMemberRole.MEMBER)
            .tokenHash("hash456")
            .invitedBy(invitedBy)
            .expiresAt(Instant.now().minusSeconds(3600))
            .build();

        invitationRepository.saveAndFlush(inv1);
        invitationRepository.saveAndFlush(invExpired);
        entityManager.clear();

        var pending = invitationRepository.findPendingByBoardId(tenantId, boardId, Instant.now());
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEmail()).isEqualTo("budi@contoh.id");
    }
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.repository.BoardInvitationRepositoryTest"`
Expected: FAIL (class `BoardInvitation` & `BoardInvitationRepository` belum ada).

- [x] **Step 3: Implementasikan BoardInvitationResult, BoardInvitation, dan BoardInvitationRepository**

Buat `BoardInvitationResult.java`:
```java
package com.hyperdata.proms.module.work.board.constant;

public final class BoardInvitationResult {
    public static final String ADDED = "added";
    public static final String INVITED = "invited";
    public static final String ALREADY_MEMBER = "already_member";
    public static final String INVALID = "invalid";

    private BoardInvitationResult() {}
}
```

Buat `BoardInvitation.java`:
```java
package com.hyperdata.proms.module.work.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "board_invitations", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class BoardInvitation {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "board_role", nullable = false)
    private String boardRole;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "workstream_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] workstreamIds = new UUID[0];

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "email_failed_at")
    private Instant emailFailedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (expiresAt == null) expiresAt = now.plusSeconds(7 * 24 * 3600);
    }
}
```

Buat `BoardInvitationRepository.java`:
```java
package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.BoardInvitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardInvitationRepository extends JpaRepository<BoardInvitation, UUID> {

    @Query("""
        SELECT bi FROM BoardInvitation bi
        WHERE bi.tenantId = :tenantId
          AND bi.boardId = :boardId
          AND bi.acceptedAt IS NULL
          AND bi.revokedAt IS NULL
          AND bi.expiresAt > :now
        ORDER BY bi.createdAt DESC
    """)
    List<BoardInvitation> findPendingByBoardId(
        @Param("tenantId") UUID tenantId,
        @Param("boardId") UUID boardId,
        @Param("now") Instant now
    );

    @Query("""
        SELECT bi FROM BoardInvitation bi
        WHERE bi.tenantId = :tenantId
          AND bi.boardId = :boardId
          AND bi.email = :email
          AND bi.acceptedAt IS NULL
          AND bi.revokedAt IS NULL
          AND bi.expiresAt > :now
    """)
    Optional<BoardInvitation> findPendingByEmail(
        @Param("tenantId") UUID tenantId,
        @Param("boardId") UUID boardId,
        @Param("email") String email,
        @Param("now") Instant now
    );
}
```

- [x] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.repository.BoardInvitationRepositoryTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 1**

Run: `./gradlew spotlessApply`

---

### Task 2: DTOs & Validation untuk Board Members & Invitations

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/InviteBoardMembersRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/UpdateBoardMemberRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardMemberListResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardMemberItemDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BoardUserDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/PendingInvitationDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/BatchInviteResultResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/work/board/dto/InviteItemResultDto.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/InviteBoardMembersRequestValidationTest.java`

**Interfaces:**
- Consumes: YAVI validation framework
- Produces: DTO records untuk transfer data member & hasil invite batch

- [x] **Step 1: Tulis unit test validasi InviteBoardMembersRequest**

```java
package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.dto.InviteBoardMembersRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InviteBoardMembersRequestValidationTest {

    @Test
    @DisplayName("Request invite valid harus lolos validasi YAVI")
    void validRequestShouldPass() {
        var req = new InviteBoardMembersRequest(
            List.of("budi@contoh.id", "siti@contoh.id"),
            BoardMemberRole.MEMBER,
            List.of()
        );
        var violations = InviteBoardMembersRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    @DisplayName("Request invite dengan emails kosong harus gagal validasi")
    void emptyEmailsShouldFail() {
        var req = new InviteBoardMembersRequest(
            List.of(),
            BoardMemberRole.MEMBER,
            List.of()
        );
        var violations = InviteBoardMembersRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("emails"));
    }
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.InviteBoardMembersRequestValidationTest"`
Expected: FAIL (class `InviteBoardMembersRequest` belum ada).

- [x] **Step 3: Implementasikan seluruh DTO Member & Invitation**

Buat `InviteBoardMembersRequest.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Payload undangan member board")
public record InviteBoardMembersRequest(
    @Schema(description = "Daftar email yang diundang", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> emails,
    @Schema(description = "Role yang diberikan (admin, pm, member, viewer, guest)", example = "member", requiredMode = Schema.RequiredMode.REQUIRED)
    String role,
    @Schema(description = "Daftar UUID workstream yang dialokasikan")
    List<UUID> workstreamIds
) {
    public static final Validator<InviteBoardMembersRequest> VALIDATOR = ValidatorBuilder.<InviteBoardMembersRequest>of()
        .constraint(InviteBoardMembersRequest::emails, "emails", c -> c.notEmpty())
        .constraint(InviteBoardMembersRequest::role, "role", c -> c.notBlank())
        .build();
}
```

Buat `UpdateBoardMemberRequest.java`:
```java
package com.hyperdata.proms.module.work.board.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Payload pembaruan role atau workstream member board")
public record UpdateBoardMemberRequest(
    @Schema(description = "Role baru pada board", example = "member")
    String role,
    @Schema(description = "Daftar UUID workstream yang dialokasikan")
    List<UUID> workstreamIds
) {
    public static final Validator<UpdateBoardMemberRequest> VALIDATOR = ValidatorBuilder.<UpdateBoardMemberRequest>of()
        .build();
}
```

Buat DTO Response DTOs:
```java
package com.hyperdata.proms.module.work.board.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoardUserDto(UUID id, String fullName, String avatarUrl, String email) {}
public record PendingInvitationDto(UUID id, String email, String role, Instant expiresAt) {}
public record BoardMemberItemDto(UUID id, BoardUserDto user, String role, boolean isOwner, List<UUID> workstreamIds, String version) {}
public record BoardMemberListResponse(List<BoardMemberItemDto> data, List<PendingInvitationDto> pendingInvitations) {}
public record InviteItemResultDto(String email, String result, UUID memberId, UUID invitationId) {}
public record BatchInviteResultResponse(List<InviteItemResultDto> results) {}
```

- [x] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.InviteBoardMembersRequestValidationTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 2**

Run: `./gradlew spotlessApply`

---

### Task 3: Ekstensi Kontrak Client (UserClient & TaskClient)

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/identity/client/contract/UserClient.java`
- Modify: `src/main/java/com/hyperdata/proms/module/identity/client/impl/UserClientImpl.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/client/contract/TaskClient.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/client/impl/TaskClientImpl.java`

**Interfaces:**
- Consumes: Modul `identity.user` dan `work.task`
- Produces: Method `findUsersByEmails` pada `UserClient` dan `unassignUserFromBoardTasks` pada `TaskClient`

- [x] **Step 1: Tambahkan method findUsersByEmails di UserClient**

Di `UserClient.java`:
```java
Map<String, UserSummaryDto> findUsersByEmails(Collection<String> emails);
```
Implementasikan di `UserClientImpl.java` menggunakan lookup repository pengguna berdasarkan email.

- [x] **Step 2: Tambahkan method unassignUserFromBoardTasks di TaskClient**

Di `TaskClient.java`:
```java
void unassignUserFromBoardTasks(UUID boardId, UUID userId);
```
Implementasikan di `TaskClientImpl.java` (mengupdate task yang ditugaskan ke user di board terkait menjadi unassigned).

- [x] **Step 3: Verifikasi kompilasi interface**

Run: `./gradlew compileJava`
Expected: SUCCESS

---

### Task 4: Service Layer Kolaborasi Member & Aturan BR-DL-04

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/service/BoardService.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/repository/BoardMemberRepository.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/BoardServiceTest.java`

**Interfaces:**
- Consumes: `BoardMemberRepository`, `BoardInvitationRepository`, `UserClient`, `TaskClient`
- Produces: Logika bisnis `getBoardMembers`, `inviteMembers`, `updateMember`, dan `removeMember` (dengan unassign BR-DL-04)

- [x] **Step 1: Tulis unit test untuk member operations di BoardServiceTest**

```java
@Test
@DisplayName("removeMember harus menghapus member dan memanggil taskClient.unassignUserFromBoardTasks")
void removeMemberShouldDeleteAndUnassignTasks() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID boardId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    CurrentUser user = new CurrentUser(userId, tenantId, "Admin", "admin@hyperdata.co.id", List.of("ADMIN"));

    BoardMember member = new BoardMember();
    member.setId(memberId);
    member.setBoardId(boardId);
    member.setTenantId(tenantId);
    member.setUserId(UUID.randomUUID());
    member.setRole("member");
    member.setUpdatedAt(Instant.parse("2026-11-19T10:00:00Z"));

    when(boardMemberRepository.findByIdAndTenantId(memberId, tenantId)).thenReturn(Optional.of(member));

    boardService.removeMember(user, boardId, memberId, "2026-11-19T10:00:00Z");

    verify(boardMemberRepository).delete(member);
    verify(taskClient).unassignUserFromBoardTasks(boardId, member.getUserId());
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.BoardServiceTest"`
Expected: FAIL (method member belum ada).

- [x] **Step 3: Implementasikan logika bisnis di BoardService**

Tambahkan method di `BoardService.java`:
1. `getBoardMembers(CurrentUser user, UUID boardId)`:
   - Ambil `board_members` dari repo.
   - Ambil pending invitations dari `invitationRepository.findPendingByBoardId(...)`.
   - Batch lookup profil user via `userClient.getUserSummaries(...)`.
   - Return `BoardMemberListResponse`.
2. `inviteMembers(CurrentUser user, UUID boardId, InviteBoardMembersRequest request)`:
   - Panggil `userClient.findUsersByEmails(request.emails())`.
   - Loop per email: jika user sudah terdaftar di tenant $\rightarrow$ tambahkan langsung ke `BoardMember` (status `added`); jika belum terdaftar $\rightarrow$ buat `BoardInvitation` (status `invited`).
   - Return `BatchInviteResultResponse`.
3. `updateMember(CurrentUser user, UUID boardId, UUID memberId, UpdateBoardMemberRequest request, String ifMatch)`:
   - Validasi `If-Match`. Update role dan workstreams.
   - Return `BoardMemberItemDto`.
4. `removeMember(CurrentUser user, UUID boardId, UUID memberId, String ifMatch)`:
   - Validasi `If-Match`. Hapus member dari `boardMemberRepository`.
   - Panggil `taskClient.unassignUserFromBoardTasks(boardId, member.getUserId())`.

- [x] **Step 4: Jalankan test dan verifikasi kelulusan (Green)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.BoardServiceTest"`
Expected: PASS

- [x] **Step 5: Verifikasi deliverable Task 4**

Run: `./gradlew spotlessApply`

---

### Task 5: Controller Endpoints & OpenAPI Documentation (API-23, API-52, API-53, API-54)

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/controller/BoardApi.java`
- Modify: `src/main/java/com/hyperdata/proms/module/work/board/controller/BoardController.java`
- Test: `src/test/java/com/hyperdata/proms/module/work/board/controller/BoardControllerTest.java`

**Interfaces:**
- Consumes: `BoardService`
- Produces: REST Endpoints `/api/v1/boards/{boardId}/members` dan `/api/v1/boards/{boardId}/invitations`

- [x] **Step 1: Tulis MockMvc controller tests untuk member endpoints**

Tambahkan pengujian di `BoardControllerTest.java`:
```java
@Test
@DisplayName("POST /api/v1/boards/{id}/invitations harus menghasilkan 200 OK")
void inviteMembersShouldReturn200() throws Exception {
    var req = new InviteBoardMembersRequest(List.of("budi@contoh.id"), "member", List.of());
    when(boardService.inviteMembers(any(), any(), any())).thenReturn(new BatchInviteResultResponse(List.of()));

    mockMvc.perform(post("/api/v1/boards/" + UUID.randomUUID() + "/invitations")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk());
}

@Test
@DisplayName("DELETE /api/v1/boards/{boardId}/members/{memberId} tanpa If-Match harus ditolak 428")
void deleteMemberWithoutIfMatchShouldReturn428() throws Exception {
    mockMvc.perform(delete("/api/v1/boards/" + UUID.randomUUID() + "/members/" + UUID.randomUUID()))
        .andExpect(status().isPreconditionRequired());
}
```

- [x] **Step 2: Jalankan test dan verifikasi kegagalan (Red)**

Run: `./gradlew test --tests "com.hyperdata.proms.module.work.board.controller.BoardControllerTest"`
Expected: FAIL (endpoints belum terdaftar di `BoardApi`).

- [x] **Step 3: Daftarkan endpoints di BoardApi dan delegasikan di BoardController**

Di `BoardApi.java`:
```java
@Operation(summary = "Daftar Anggota Board", description = "Mengambil daftar anggota board aktif dan undangan tertunda")
@GetMapping("/{boardId}/members")
RestResponse<BoardMemberListResponse> getBoardMembers(@PathVariable("boardId") UUID boardId);

@Operation(summary = "Undang Anggota ke Board", description = "Mengundang anggota ke board via email")
@PostMapping("/{boardId}/invitations")
RestResponse<BatchInviteResultResponse> inviteBoardMembers(
    @PathVariable("boardId") UUID boardId,
    @RequestHeader("Idempotency-Key") UUID idempotencyKey,
    @RequestBody InviteBoardMembersRequest request);

@Operation(summary = "Perbarui Role/Workstream Anggota", description = "Memperbarui peran atau alokasi workstream anggota board")
@ApiConcurrencyControl
@PatchMapping("/{boardId}/members/{memberId}")
RestResponse<BoardMemberItemDto> updateBoardMember(
    @PathVariable("boardId") UUID boardId,
    @PathVariable("memberId") UUID memberId,
    @RequestHeader("If-Match") String ifMatch,
    @RequestBody UpdateBoardMemberRequest request);

@Operation(summary = "Keluarkan Anggota dari Board", description = "Mengeluarkan anggota dan melepaskan penugasan task")
@ApiConcurrencyControl
@DeleteMapping("/{boardId}/members/{memberId}")
RestResponse<Void> removeBoardMember(
    @PathVariable("boardId") UUID boardId,
    @PathVariable("memberId") UUID memberId,
    @RequestHeader("If-Match") String ifMatch);
```

Implementasikan pada `BoardController.java`.

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
Expected: BUILD SUCCESSFUL (ArchUnit boundary rules lulus).

- [x] **Step 3: Jalankan format code Spotless**

Run: `./gradlew spotlessApply && ./gradlew spotlessCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: Jalankan full quality gate check**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL

