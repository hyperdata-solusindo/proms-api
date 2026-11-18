package com.hyperdata.proms.module.pm.focus.repository.spec;

import com.hyperdata.proms.module.pm.focus.entity.FocusSession;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class FocusSpecs {

    private FocusSpecs() {
    }

    public static Specification<FocusSession> userSessionsInRange(
        UUID tenantId, UUID userId, Instant from, Instant to) {
        return hasTenantId(tenantId)
            .and(hasUserId(userId))
            .and(startedAtFrom(from))
            .and(startedAtBefore(to));
    }

    public static Specification<FocusSession> hasTenantId(UUID tenantId) {
        return (root, query, cb) -> tenantId == null
            ? cb.conjunction()
            : cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<FocusSession> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    /** startedAt >= from (inklusif). */
    public static Specification<FocusSession> startedAtFrom(Instant from) {
        return (root, query, cb) -> from == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("startedAt"), from);
    }

    public static Specification<FocusSession> startedAtBefore(Instant to) {
        return (root, query, cb) -> to == null
            ? cb.conjunction()
            : cb.lessThan(root.get("startedAt"), to);
    }
}
