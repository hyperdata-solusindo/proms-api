package com.hyperdata.proms.module.pm.standup.repository.spec;

import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class StandupSpecs {

    public StandupSpecs() {
    }

    public static Specification<StandupReports> userReportOnDate(UUID tenantId, UUID userId, LocalDate reportDate) {
        return hasTenantId(tenantId)
            .and(hasUserId(userId))
            .and(hasReportDate(reportDate));
    }

    public static Specification<StandupReports> reportsForTenantOnDate(UUID tenantId, LocalDate reportDate) {
        return hasTenantId(tenantId)
            .and(hasReportDate(reportDate));
    }

    public static Specification<StandupReports> ownedById(UUID tenantId, UUID userId, UUID id) {
        return hasTenantId(tenantId)
            .and(hasUserId(userId))
            .and(hasId(id));
    }

    public static Specification<StandupReports> hasId(UUID id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    public static Specification<StandupReports> hasTenantId(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<StandupReports> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<StandupReports> hasReportDate(LocalDate reportDate) {
        return (root, query, cb) -> cb.equal(root.get("reportDate"), reportDate);
    }
}
