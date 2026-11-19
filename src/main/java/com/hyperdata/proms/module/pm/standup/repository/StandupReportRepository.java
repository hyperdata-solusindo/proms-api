package com.hyperdata.proms.module.pm.standup.repository;

import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StandupReportRepository
    extends
        JpaRepository<StandupReports, UUID>,
        JpaSpecificationExecutor<StandupReports> {

    @Query("""
            SELECT report FROM StandupReports report
            WHERE report.tenantId = :tenantId
              AND report.userId = :userId
              AND report.isLate = true
            ORDER BY report.reportDate DESC, report.submittedAt DESC
        """)
    List<StandupReports> findLateReportsByTenantIdAndUserId(
        @Param("tenantId")
        UUID tenantId,
        @Param("userId")
        UUID userId);

    @Query("""
            SELECT report FROM StandupReports report
            WHERE report.tenantId = :tenantId
            AND report.userId = :userId
            AND report.reportDate >= :from
            AND report.reportDate <= :to
            ORDER BY report.reportDate DESC, report.submittedAt DESC
        """)
    List<StandupReports> findByTenantIdAndUserIdAndDateRange(
        @Param("tenantId")
        UUID tenantId,
        @Param("userId")
        UUID userId,
        @Param("from")
        LocalDate from,
        @Param("to")
        LocalDate to);
}
