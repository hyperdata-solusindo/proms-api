# Spesifikasi Desain Arsitektur: Modul Board Fase 2 (Member Collaboration & Invitasi)

- **Tanggal Dokumen**: 2026-11-19
- **Domain & Modul**: `com.hyperdata.proms.module.work.board`
- **Referensi Desain**: Dokumen `mobile-v1-api-mapping-spring` (Figma Mobile `07-invite`, `07-member`, `10`) & `board-endpoints-flow-and-relations.md`
- **Status**: Draft untuk Review User

---

## 1. Ringkasan Eksekutif & Tujuan

Spesifikasi ini mendefinisikan implementasi **Fase 2** dari modul Board di domain `work`. Fase 2 berfokus pada kolaborasi dan keanggotaan board:
1. **API-23**: `GET /api/v1/boards/{boardId}/members` (Daftar anggota board aktif dan undangan tertunda/pending).
2. **API-52**: `POST /api/v1/boards/{boardId}/invitations` (Undang anggota via email; langsung assign jika sudah terdaftar sebagai user atau buat invitation token jika belum terdaftar).
3. **API-53**: `PATCH /api/v1/boards/{boardId}/members/{memberId}` (Ubah role anggota board atau alokasi workstream).
4. **API-54**: `DELETE /api/v1/boards/{boardId}/members/{memberId}` (Keluarkan anggota board + lepaskan/unassign task agar tidak menjadi orphan sesuai aturan **BR-DL-04**).

---

## 2. Kepatuhan Master Directives (AGENTS.md)

1. **Inter-Module Client Contract**:
   - Berinteraksi dengan `UserClient` (`identity.user`) untuk batch lookup data pengguna (`getUserSummaries`) dan validasi email (`findUsersByEmails`).
   - Berinteraksi dengan `TaskClient` (`work.task`) saat member dikeluarkan untuk unassign task: `taskClient.unassignUserFromBoardTasks(boardId, userId)`.
2. **PostgreSQL RLS & UUIDv7**:
   - Entitas baru `BoardInvitation` wajib menggunakan `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` dan proteksi multi-tenancy `tenant_id`.
3. **Optimistic Locking**:
   - `PATCH /boards/{boardId}/members/{memberId}` dan `DELETE .../members/{memberId}` memvalidasi header `If-Match` dengan `updatedAt` / epoch timestamp entity member.
4. **Idempotency**:
   - Operasi `POST /invitations` mendukung header `Idempotency-Key` untuk mencegah pengiriman undangan/email ganda.

---

## 3. Data Model & Persistensi (JPA Entities)

### Entitas Baru: `BoardInvitation`
Memetakan tabel PostgreSQL `proms.board_invitations`:

```java
package com.hyperdata.proms.module.work.board.entity;

import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
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
    private String boardRole; // admin, pm, member, viewer, guest

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
        if (expiresAt == null) expiresAt = now.plusSeconds(7 * 24 * 3600); // 7 hari
    }

    public boolean isPending() {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
```

---

## 4. Spesifikasi Kontrak DTO & Endpoint

### A. API-23: `GET /api/v1/boards/{boardId}/members`
- **Response**: `RestResponse<BoardMemberListResponse>`
```java
public record BoardMemberListResponse(
    List<BoardMemberItemDto> data,
    List<PendingInvitationDto> pendingInvitations
) {}

public record BoardMemberItemDto(
    UUID id,
    BoardUserDto user,
    String role,
    boolean isOwner,
    List<UUID> workstreamIds,
    String version
) {}

public record BoardUserDto(
    UUID id,
    String fullName,
    String avatarUrl,
    String email
) {}

public record PendingInvitationDto(
    UUID id,
    String email,
    String role,
    Instant expiresAt
) {}
```

### B. API-52: `POST /api/v1/boards/{boardId}/invitations` (Undang Member)
- **Header**: `Idempotency-Key: <UUID>` (Wajib)
- **Request Record**: `InviteBoardMembersRequest`
```java
public record InviteBoardMembersRequest(
    List<String> emails,
    String role,
    List<UUID> workstreamIds
) {
    public static final Validator<InviteBoardMembersRequest> VALIDATOR = ValidatorBuilder.<InviteBoardMembersRequest>of()
        .constraint(InviteBoardMembersRequest::emails, "emails", c -> c.notEmpty().greaterThanOrEqual(1))
        .constraint(InviteBoardMembersRequest::role, "role", c -> c.notBlank())
        .build();
}
```
- **Logika Eksekusi**:
  1. Validasi otorisasi: User yang mengundang harus memiliki role `admin` atau `pm` di board tersebut.
  2. Panggil batch `userClient.findUsersByEmails(emails)`.
  3. Untuk email yang sudah ada akun user:
     - Jika sudah menjadi member board $\rightarrow$ status `already_member`.
     - Jika belum $\rightarrow$ insert ke `proms.board_members` + alokasikan workstreams $\rightarrow$ status `added`.
  4. Untuk email yang belum punya akun:
     - Generate token undangan securely $\rightarrow$ simpan ke `proms.board_invitations` $\rightarrow$ status `invited`.
- **Response**: `200 OK` atau `207 Multi-Status` $\rightarrow$ `RestResponse<BatchInviteResultResponse>`
```java
public record BatchInviteResultResponse(
    List<InviteItemResultDto> results
) {}

public record InviteItemResultDto(
    String email,
    String result,         // "added", "invited", "already_member", "invalid"
    UUID memberId,
    UUID invitationId
) {}
```

### C. API-53: `PATCH /api/v1/boards/{boardId}/members/{memberId}` (Update Role & Workstream)
- **Header**: `If-Match: <version>` (Wajib)
- **Request Record**: `UpdateBoardMemberRequest`
```java
public record UpdateBoardMemberRequest(
    String role,
    List<UUID> workstreamIds
) {}
```
- **Response**: `RestResponse<BoardMemberItemDto>`

### D. API-54: `DELETE /api/v1/boards/{boardId}/members/{memberId}` (Keluarkan Member)
- **Header**: `If-Match: <version>` (Wajib)
- **Logika Eksekusi**:
  1. Validasi otorisasi (hanya Admin/PM atau user itu sendiri yang leave board).
  2. Owner board tidak boleh dihapus jika belum transfer ownership.
  3. Hapus baris member dari `proms.board_members` dan `proms.board_member_workstreams`.
  4. **Penting (BR-DL-04)**: Panggil `taskClient.unassignUserFromBoardTasks(boardId, member.userId)` agar task yang ditinggalkan status assignee-nya menjadi unassigned/kosong, bukan invalid UUID.
- **Response**: `RestResponse<Void>` (`statusCode: 200`, `data: null`).

