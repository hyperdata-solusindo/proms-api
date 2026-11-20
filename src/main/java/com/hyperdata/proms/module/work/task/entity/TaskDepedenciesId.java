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
public class TaskDepedenciesId {

    @Column(name = "blocking_task_id", nullable = false)
    private UUID blockingTaskId;

    @Column(name = "blocked_task_id", nullable = false)
    private UUID blockedTaskId;

}
