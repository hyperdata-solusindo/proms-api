package com.hyperdata.proms.module.pm.standup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.pm.standup.constant.StandupAttendanceStatus;
import com.hyperdata.proms.module.pm.standup.constant.StandupRequirementReason;
import com.hyperdata.proms.module.pm.standup.dto.StandupDetailResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupListResponse;
import com.hyperdata.proms.module.pm.standup.entity.StandupPolicies;
import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import com.hyperdata.proms.module.pm.standup.mapper.StandupMapper;
import com.hyperdata.proms.module.pm.standup.repository.StandupPoliciesRepository;
import com.hyperdata.proms.module.pm.standup.repository.StandupReminderRunsRepository;
import com.hyperdata.proms.module.pm.standup.repository.StandupReportRepository;
import com.hyperdata.proms.module.pm.standup.service.StandupService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StandupServiceTest {

    @Mock
    private StandupPoliciesRepository policiesRepository;

    @Mock
    private StandupReportRepository reportsRepository;

    @Mock
    private StandupReminderRunsRepository standupReminderRunsRepository;

    @Mock
    private TenantClient tenantClient;

    @Mock
    private UserClient userClient;

    @Mock
    private StandupMapper standupMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private StandupService standupService;

    private UUID tenantId;
    private CurrentUser currentUser;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        currentUser = new CurrentUser(UUID.randomUUID(), "standup-test@example.test", tenantId, "member", 1L);
        today = LocalDate.now(ZoneId.of("Asia/Jakarta"));
    }

    @Test
    @DisplayName("Given active standup policy, when getStandups, then returns roster and daily summary")
    void givenActivePolicy_whenGetStandups_thenReturnsRosterAndSummary() {
        givenTenantDetails();
        UserSummaryDto submittedUser = new UserSummaryDto(UUID.randomUUID(), "Ayu Test", null, null);
        UserSummaryDto missingUser = new UserSummaryDto(UUID.randomUUID(), "Bima Test", null, null);
        StandupReports report = reportFor(submittedUser.id(), true, "Need help");
        given(policiesRepository.findById(tenantId))
            .willReturn(Optional.of(StandupPolicies.builder().tenantId(tenantId).enabled(true).build()));
        given(reportsRepository.findAll(any(Specification.class))).willReturn(List.of(report));
        given(userClient.findActiveSummariesByTenantId(tenantId)).willReturn(List.of(missingUser, submittedUser));
        given(standupMapper.toDetailResponse(report)).willReturn(new StandupDetailResponse(
            report.getId(),
            today,
            "Plan",
            "Done",
            "Need help",
            report.getSubmittedAt(),
            true,
            "version"));

        StandupListResponse response = standupService.getStandups(currentUser);

        assertThat(response.date()).isEqualTo(today);
        assertThat(response.summary().submitted()).isEqualTo(1);
        assertThat(response.summary().late()).isEqualTo(1);
        assertThat(response.summary().missing()).isEqualTo(1);
        assertThat(response.summary().exempt()).isZero();
        assertThat(response.data()).extracting(entry -> entry.status())
            .containsExactly(StandupAttendanceStatus.SUBMITTED, StandupAttendanceStatus.MISSING);
        assertThat(response.data().get(0).hasBlockers()).isTrue();
        assertThat(response.data().get(1).report()).isNull();
    }

    @Test
    @DisplayName("Given disabled standup policy, when getStandups, then marks members exempt")
    void givenDisabledPolicy_whenGetStandups_thenMarksMembersExempt() {
        givenTenantDetails();
        UserSummaryDto user = new UserSummaryDto(UUID.randomUUID(), "Citra Test", null, null);
        given(policiesRepository.findById(tenantId))
            .willReturn(Optional.of(StandupPolicies.builder().tenantId(tenantId).enabled(false).build()));
        given(reportsRepository.findAll(any(Specification.class))).willReturn(List.of());
        given(userClient.findActiveSummariesByTenantId(tenantId)).willReturn(List.of(user));

        StandupListResponse response = standupService.getStandups(currentUser);

        assertThat(response.summary().exempt()).isEqualTo(1);
        assertThat(response.data().get(0).status()).isEqualTo(StandupAttendanceStatus.EXEMPT);
        assertThat(response.data().get(0).reason()).isEqualTo(StandupRequirementReason.DISABLED);
    }

    @Test
    @DisplayName("Given late reports, when getMyLateStandups, then returns reports marked late")
    void givenLateReports_whenGetMyLateStandups_thenReturnsLateReports() {
        StandupReports report = reportFor(UUID.randomUUID(), true, null);
        StandupDetailResponse responseDto = new StandupDetailResponse(
            report.getId(),
            today,
            "Plan",
            "Done",
            null,
            report.getSubmittedAt(),
            true,
            "version");
        given(reportsRepository.findLateReportsByTenantIdAndUserId(tenantId, currentUser.userId()))
            .willReturn(List.of(report));
        given(standupMapper.toDetailResponse(report)).willReturn(responseDto);

        List<StandupDetailResponse> responses = standupService.getMyLateStandups(currentUser);

        assertThat(responses).containsExactly(responseDto);
        assertThat(responses.get(0).isLate()).isTrue();
    }

    private StandupReports reportFor(UUID userId, boolean late, String blockers) {
        StandupReports report = new StandupReports();
        report.setId(UUID.randomUUID());
        report.setTenantId(tenantId);
        report.setUserId(userId);
        report.setReportDate(today);
        report.setSubmittedAt(Instant.now());
        report.setLate(late);
        report.setBlockers(blockers);
        return report;
    }

    private void givenTenantDetails() {
        given(tenantClient.getTenantDetail(tenantId))
            .willReturn(Optional.of(new TenantDetailDto(tenantId, "tenant-test", "Tenant Test", "active",
                "Asia/Jakarta")));
    }
}
