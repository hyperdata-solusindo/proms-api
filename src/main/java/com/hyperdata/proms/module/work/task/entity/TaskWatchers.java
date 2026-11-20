package com.hyperdata.proms.module.work.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_watchers", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class TaskWatchers {

    @EmbeddedId
    private TaskWatchersId id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
