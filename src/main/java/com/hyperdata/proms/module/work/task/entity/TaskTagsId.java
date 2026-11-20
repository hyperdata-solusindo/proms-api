package com.hyperdata.proms.module.work.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TaskTagsId {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;
}
