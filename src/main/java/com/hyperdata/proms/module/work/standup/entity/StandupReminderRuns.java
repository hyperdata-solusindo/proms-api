package com.hyperdata.proms.module.work.standup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "standup_reminder_runs", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class StandupReminderRuns {

    @EmbeddedId
    private StandupReminderRunsId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
