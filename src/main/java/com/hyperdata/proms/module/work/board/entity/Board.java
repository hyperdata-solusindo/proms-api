package com.hyperdata.proms.module.work.board.entity;

import com.hyperdata.proms.module.work.board.constant.BoardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @org.hibernate.annotations.UuidGenerator(style = org.hibernate.annotations.UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "board_group_id")
    private UUID boardGroupId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "pm_id")
    private UUID pmId;

    @Column(name = "status", nullable = false)
    private String status = BoardStatus.ACTIVE;

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
    private Integer progressPct = 0;

    public Integer getProgressPct() {
        return progressPct != null ? progressPct : 0;
    }

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
