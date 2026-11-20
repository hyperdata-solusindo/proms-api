package com.hyperdata.proms.module.work.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_feedback")
@Getter
@Setter
@NoArgsConstructor
public class TaskFeedback {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "score", nullable = false, updatable = false)
    private Short score;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Column(name = "givenBy")
    private UUID givenBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "superseded_at")
    private Instant supersededAt;
}
