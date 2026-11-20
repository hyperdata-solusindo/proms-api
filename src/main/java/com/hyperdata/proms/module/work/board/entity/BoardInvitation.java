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
    @Column(name = "workstream_ids")
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
        if (createdAt == null) {
            createdAt = now;
        }
        if (expiresAt == null) {
            expiresAt = now.plusSeconds(7 * 24 * 3600);
        }
    }
}
