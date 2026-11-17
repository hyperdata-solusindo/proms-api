package com.hyperdata.proms.module.pm.standup.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.IfMatchRequiredException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.exception.StaleVersionException;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.pm.standup.constant.StandupAttendanceStatus;
import com.hyperdata.proms.module.pm.standup.constant.StandupRequirementReason;
import com.hyperdata.proms.module.pm.standup.dto.CreateStandupRequest;
import com.hyperdata.proms.module.pm.standup.dto.StandupDetailResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupEntryResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupListResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupSummaryResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupTodayResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupUpdateResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupUserResponse;
import com.hyperdata.proms.module.pm.standup.dto.UpdateStandupRequest;
import com.hyperdata.proms.module.pm.standup.entity.StandupPolicies;
import com.hyperdata.proms.module.pm.standup.entity.StandupReminderRuns;
import com.hyperdata.proms.module.pm.standup.entity.StandupReminderRunsId;
import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import com.hyperdata.proms.module.pm.standup.mapper.StandupMapper;
import com.hyperdata.proms.module.pm.standup.repository.StandupPoliciesRepository;
import com.hyperdata.proms.module.pm.standup.repository.StandupReminderRunsRepository;
import com.hyperdata.proms.module.pm.standup.repository.StandupReportRepository;
import com.hyperdata.proms.module.pm.standup.repository.spec.StandupSpecs;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandupService {

    private final StandupPoliciesRepository policiesRepository;
    private final StandupReportRepository reportsRepository;
    private final StandupReminderRunsRepository standupReminderRunsRepository;
    private final TenantClient tenantClient;
    private final UserClient userClient;
    private final StandupMapper standupMapper;
    private final EntityManager entityManager;

    public StandupListResponse getStandups(CurrentUser currentUser) {
        UUID tenantId = currentUser.activeTenantId();
        LocalDate today = tenantNow(tenantId, Instant.now()).toLocalDate();
        StandupPolicies policy = findPolicy(tenantId);
        List<StandupReports> reports = reportsRepository.findAll(StandupSpecs.reportsForTenantOnDate(tenantId, today));
        Map<UUID, StandupReports> reportsByUserId = Lookups.toMap(reports, StandupReports::getUserId);
        List<UserSummaryDto> users = userClient.findActiveSummariesByTenantId(tenantId)
            .stream()
            .sorted(Comparator.comparing(UserSummaryDto::name))
            .toList();

        List<StandupEntryResponse> entries = users.stream()
            .map(user -> toStandupEntry(user, reportsByUserId.get(user.id()), policy.isEnabled()))
            .toList();
        int submitted = (int) entries.stream()
            .filter(entry -> StandupAttendanceStatus.SUBMITTED.equals(entry.status()))
            .count();
        int late = (int) entries.stream()
            .filter(entry -> entry.report() != null && entry.report().isLate())
            .count();
        int missing = (int) entries.stream()
            .filter(entry -> StandupAttendanceStatus.MISSING.equals(entry.status()))
            .count();
        int exempt = (int) entries.stream()
            .filter(entry -> StandupAttendanceStatus.EXEMPT.equals(entry.status()))
            .count();

        return new StandupListResponse(today, new StandupSummaryResponse(submitted, late, missing, exempt), entries);
    }

    public StandupTodayResponse getToday(CurrentUser currentUser) {
        UUID tenantId = currentUser.activeTenantId();
        LocalDate today = tenantNow(tenantId, Instant.now()).toLocalDate();
        StandupPolicies policy = findPolicy(tenantId);
        StandupReports report = reportsRepository
            .findOne(StandupSpecs.userReportOnDate(tenantId, currentUser.userId(), today))
            .orElse(null);

        boolean required = true;
        String reason = null;
        if (!policy.isEnabled()) {
            required = false;
            reason = StandupRequirementReason.DISABLED;
        } else if (report != null) {
            required = false;
            reason = StandupRequirementReason.ALREADY_SUBMITTED;
        }
        return standupMapper.toTodayResponse(today, required, reason, report, policy);
    }

    public List<StandupDetailResponse> getMyLateStandups(CurrentUser currentUser) {
        return reportsRepository
            .findLateReportsByTenantIdAndUserId(currentUser.activeTenantId(), currentUser.userId())
            .stream()
            .map(standupMapper::toDetailResponse)
            .toList();
    }

    @Transactional
    public StandupDetailResponse createStandup(CurrentUser currentUser, CreateStandupRequest request) {
        UUID tenantId = currentUser.activeTenantId();
        UUID userId = currentUser.userId();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        ZonedDateTime tenantNow = tenantNow(tenantId, now);
        StandupPolicies policy = findOrCreatePolicy(tenantId);

        if (!policy.isEnabled()) {
            throw new BusinessException("Standup tidak aktif untuk tenant ini");
        }

        LocalDate today = tenantNow.toLocalDate();
        if (reportsRepository.findOne(StandupSpecs.userReportOnDate(tenantId, userId, today)).isPresent()) {
            throw new DuplicateResourceException("Standup untuk tanggal " + today + " sudah dikirim");
        }

        StandupReports report = standupMapper.toEntity(request);
        report.setId(UUID.randomUUID());
        report.setTenantId(tenantId);
        report.setUserId(userId);
        report.setReportDate(today);
        report.setSubmittedAt(now);
        report.setLate(tenantNow.toLocalTime().isAfter(policy.getDeadlineTime()));
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        StandupReports saved = reportsRepository.saveAndFlush(report);
        recordReminderRun(tenantId, today);
        log.info("Standup berhasil dikirim: standupId={}, tenantId={}, userId={}, tanggal={}, terlambat={}",
            saved.getId(), tenantId, userId, today, saved.isLate());
        return standupMapper.toDetailResponse(saved);
    }

    @Transactional
    public StandupUpdateResponse updateStandup(
        CurrentUser currentUser,
        UUID standupId,
        String ifMatch,
        UpdateStandupRequest request) {
        Instant expectedVersion = parseIfMatch(ifMatch);

        StandupReports report = reportsRepository
            .findOne(StandupSpecs.ownedById(currentUser.activeTenantId(), currentUser.userId(), standupId))
            .orElseThrow(() -> new ResourceNotFoundException("Standup", standupId));

        if (!report.getUpdatedAt().equals(expectedVersion)) {
            throw new StaleVersionException(standupMapper.toDetailResponse(report));
        }

        standupMapper.updateEntity(request, report);
        reportsRepository.saveAndFlush(report);
        entityManager.refresh(report);
        log.info("Standup berhasil diperbarui: standupId={}, tenantId={}, userId={}",
            standupId, currentUser.activeTenantId(), currentUser.userId());
        return standupMapper.toUpdateResponse(report);
    }

    @Transactional(readOnly = true)
    public List<StandupDetailResponse> getMyStandups(
        CurrentUser currentUser,
        LocalDate from,
        LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Tanggal from tidak boleh lebih besar dari 0");
        }

        return reportsRepository
            .findByTenantIdAndUserIdAndDateRange(
                currentUser.activeTenantId(), currentUser.userId(), from, to)
            .stream()
            .map(standupMapper::toDetailResponse)
            .toList();
    }

    private StandupEntryResponse toStandupEntry(
        UserSummaryDto user,
        StandupReports report,
        boolean enabled) {
        String status;
        String reason = null;
        if (report != null) {
            status = StandupAttendanceStatus.SUBMITTED;
        } else if (enabled) {
            status = StandupAttendanceStatus.MISSING;
        } else {
            status = StandupAttendanceStatus.EXEMPT;
            reason = StandupRequirementReason.DISABLED;
        }

        StandupDetailResponse reportResponse = report == null ? null : standupMapper.toDetailResponse(report);
        boolean hasBlockers = report != null && report.getBlockers() != null && !report.getBlockers().isBlank();
        return new StandupEntryResponse(
            new StandupUserResponse(user.id(), user.name(), user.avatarUrl()),
            status,
            reason,
            hasBlockers,
            reportResponse);
    }

    private StandupPolicies findPolicy(UUID tenantId) {
        return policiesRepository.findById(tenantId)
            .orElseGet(() -> StandupPolicies.builder().tenantId(tenantId).build());
    }

    private StandupPolicies findOrCreatePolicy(UUID tenantId) {
        return policiesRepository.findById(tenantId)
            .orElseGet(() -> policiesRepository.saveAndFlush(StandupPolicies.builder().tenantId(tenantId).build()));
    }

    private void recordReminderRun(UUID tenantId, LocalDate runDate) {
        StandupReminderRunsId id = new StandupReminderRunsId();
        id.setTenantId(tenantId);
        id.setRunDate(runDate);
        if (!standupReminderRunsRepository.existsById(id)) {
            StandupReminderRuns run = new StandupReminderRuns();
            run.setId(id);
            standupReminderRunsRepository.saveAndFlush(run);
        }
    }

    private ZonedDateTime tenantNow(UUID tenantId, Instant now) {
        String timezone = tenantClient.getTenantDetail(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId))
            .timezone();
        return now.atZone(ZoneId.of(timezone));
    }

    private Instant parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IfMatchRequiredException();
        }
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        try {
            return Instant.parse(value.replace("\"", ""));
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                CommonErrorCode.BAD_REQUEST,
                "Format header If-Match tidak valid",
                "Gunakan nilai version dari respons terakhir",
                HttpStatus.BAD_REQUEST);
        }
    }
}
