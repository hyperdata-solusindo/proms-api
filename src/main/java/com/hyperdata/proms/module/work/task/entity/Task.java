package com.hyperdata.proms.module.work.task.entity;

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
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tasks", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    // diisi trigger proms.assign_task_number(); dibaca ulang Hibernate setelah INSERT
    @Generated(event = EventType.INSERT)
    @Column(name = "number", insertable = false, updatable = false)
    private Integer number;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "workstream_id", nullable = false)
    private UUID workstreamId;

    @Column(name = "column_id", nullable = false)
    private UUID columnId;

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "title", nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description")
    private String description;

    @Column(name = "description_text")
    private String descriptionText;

    @Column(name = "priority")
    private String priority;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "is_blocked")
    private boolean isBlocked;

    @Column(name = "blocked_reason")
    private String blockedReason;

    // diisi trigger proms.sync_completed_at() saat pindah ke/dari kolom done
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "completed_at", insertable = false, updatable = false)
    private Instant completedAt;

    @Column(name = "shared_with_client", nullable = false)
    private boolean sharedWithClient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_values")
    private String customValues;

    @Column(name = "recurrence_rule_id")
    private UUID recurrenceRuleId;

    @Column(name = "source_form_id")
    private UUID sourceFormId;

    @Column(name = "source_email", columnDefinition = "citext")
    private String sourceEmail;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // diperbarui trigger platform.touch_updated_at(); dibaca ulang agar If-Match selalu cocok dengan DB
    @Generated(event = EventType.UPDATE)
    @Column(name = "updated_at", nullable = false, updatable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
