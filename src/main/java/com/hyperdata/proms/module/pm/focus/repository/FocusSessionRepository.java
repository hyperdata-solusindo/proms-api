package com.hyperdata.proms.module.pm.focus.repository;

import com.hyperdata.proms.module.pm.focus.entity.FocusSession;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FocusSessionRepository
    extends
        JpaRepository<FocusSession, UUID>,
        JpaSpecificationExecutor<FocusSession> {

    @Query("""
        select coalesce(sum(f.durationSec), 0)
        from FocusSession f
        where f.userId = :userId
          and f.startedAt >= :from
          and f.startedAt < :to
        """)
    long sumDurationSecInRange(
        @Param("userId")
        UUID userId,
        @Param("from")
        Instant from,
        @Param("to")
        Instant to);
}
