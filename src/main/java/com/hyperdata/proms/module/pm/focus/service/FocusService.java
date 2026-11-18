package com.hyperdata.proms.module.pm.focus.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.FieldErrorDetail;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.TenantScoped;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.pm.focus.constant.FocusDefaults;
import com.hyperdata.proms.module.pm.focus.constant.FocusFields;
import com.hyperdata.proms.module.pm.focus.constant.FocusLimits;
import com.hyperdata.proms.module.pm.focus.dto.CreateFocusSessionRequest;
import com.hyperdata.proms.module.pm.focus.dto.FocusSessionListResult;
import com.hyperdata.proms.module.pm.focus.dto.FocusSessionResponse;
import com.hyperdata.proms.module.pm.focus.dto.FocusSummaryResponse;
import com.hyperdata.proms.module.pm.focus.entity.FocusSession;
import com.hyperdata.proms.module.pm.focus.mapper.FocusMapper;
import com.hyperdata.proms.module.pm.focus.repository.FocusSessionRepository;
import com.hyperdata.proms.module.pm.focus.repository.spec.FocusSpecs;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@TenantScoped
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusService {

    private final FocusSessionRepository focusSessionRepository;
    private final FocusMapper focusMapper;
    private final TenantClient tenantClient;
    private final UserClient userClient;

    public FocusSessionListResult list(CurrentUser currentUser, LocalDate from, LocalDate to) {
        UUID tenantId = currentUser.activeTenantId();
        UUID userId = currentUser.userId();
        ZoneId zone = resolveZone(tenantId);

        LocalDate rangeFrom = from != null ? from : LocalDate.now(zone);
        LocalDate weekStart = rangeFrom.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate rangeTo = to != null ? to : weekEnd;

        if (rangeTo.isBefore(rangeFrom)) {
            throw new BusinessException(
                CommonErrorCode.BAD_REQUEST,
                Messages.get("focus.error.invalid_range"),
                HttpStatus.BAD_REQUEST);
        }

        Instant fromInstant = rangeFrom.atStartOfDay(zone).toInstant();
        Instant toInstant = rangeTo.plusDays(1).atStartOfDay(zone).toInstant();

        List<FocusSessionResponse> sessions = focusSessionRepository
            .findAll(
                FocusSpecs.userSessionsInRange(tenantId, userId, fromInstant, toInstant),
                Sort.by(Sort.Direction.DESC, "startedAt"))
            .stream()
            .map(s -> toResponse(s, zone))
            .toList();

        return new FocusSessionListResult(sessions, buildSummary(tenantId, userId, zone, weekStart));
    }

    @Transactional
    public FocusSessionResponse create(CurrentUser currentUser, CreateFocusSessionRequest request) {
        UUID tenantId = currentUser.activeTenantId();
        UUID userId = currentUser.userId();

        Instant startedAt = request.startedAt().truncatedTo(ChronoUnit.MICROS);
        Instant endedAt = request.endedAt().truncatedTo(ChronoUnit.MICROS);
        long seconds = Duration.between(startedAt, endedAt).getSeconds();

        if (seconds < FocusLimits.MIN_DURATION_SEC || seconds > FocusLimits.MAX_DURATION_SEC) {
            String message = Messages.get("focus.error.duration");
            throw new ValidationException(
                CommonErrorCode.BR_WF_01,
                message,
                null,
                List.of(FieldErrorDetail.of(FocusFields.ENDED_AT, message)));
        }

        FocusSession session = new FocusSession();
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setStartedAt(startedAt);
        session.setEndedAt(endedAt);
        session.setDurationSec((int) seconds);

        FocusSession saved = focusSessionRepository.saveAndFlush(session);
        log.info("Sesi fokus tersimpan: sessionId={}, tenantId={}, userId={}, durasiDetik={}",
            saved.getId(), tenantId, userId, seconds);

        // TODO(poin): setelah modul poin ada, panggil PointClient untuk BR-PT-30
        return toResponse(saved, resolveZone(tenantId));
    }

    private FocusSummaryResponse buildSummary(UUID tenantId, UUID userId, ZoneId zone, LocalDate weekStart) {
        Instant weekFrom = weekStart.atStartOfDay(zone).toInstant();
        Instant weekTo = weekStart.plusDays(7).atStartOfDay(zone).toInstant();

        long totalSec = focusSessionRepository.sumDurationSecInRange(userId, weekFrom, weekTo);
        int capacityMin = userClient.findWeeklyCapacityMin(tenantId, userId)
            .orElse(FocusDefaults.WEEKLY_CAPACITY_MIN);

        return new FocusSummaryResponse(totalSec, totalSec / 60, capacityMin, weekStart, weekStart.plusDays(6));
    }

    private FocusSessionResponse toResponse(FocusSession session, ZoneId zone) {
        LocalDate date = session.getStartedAt().atZone(zone).toLocalDate();
        // task selalu null sampai kolom task_id (migrasi MOB-B02) dan TaskClient siap
        return focusMapper.toResponse(session, date, null);
    }

    private ZoneId resolveZone(UUID tenantId) {
        String timezone = tenantClient.getTenantDetail(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId))
            .timezone();
        return ZoneId.of(timezone != null ? timezone : FocusDefaults.TIMEZONE);
    }
}
