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
