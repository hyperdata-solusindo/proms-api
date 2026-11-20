package com.hyperdata.proms.module.work.standup.entity;

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
@Table(name = "standup_reports", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
public class StandupReports {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "plan")
    private String plan;

    @Column(name = "done")
    private String done;

    @Column(name = "blockers")
    private String blockers;

    @Column(name = "submitted_at")
    private Instant submittedAt = Instant.now();

    @Column(name = "is_late")
    private boolean isLate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
