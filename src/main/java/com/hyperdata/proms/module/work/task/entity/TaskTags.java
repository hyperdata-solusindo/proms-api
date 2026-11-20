package com.hyperdata.proms.module.work.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_tags", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class TaskTags {

    @EmbeddedId
    private TaskTagsId id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
