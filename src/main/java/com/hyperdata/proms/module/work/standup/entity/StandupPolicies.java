package com.hyperdata.proms.module.work.standup.entity;

import com.hyperdata.proms.module.work.standup.constant.StandupPolicyDefaults;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "standup_policies", schema = "proms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StandupPolicies {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;

    @Builder.Default
    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = StandupPolicyDefaults.REMINDER_TIME;

    @Builder.Default
    @Column(name = "deadline_time", nullable = false)
    private LocalTime deadlineTime = StandupPolicyDefaults.DEADLINE_TIME;

    @Builder.Default
    @Column(name = "blocking_banner", nullable = false)
    private boolean blockingBanner = false;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
