package com.hyperdata.proms.module.pm.client.impl;

import com.hyperdata.proms.module.pm.client.contract.StandupClient;
import com.hyperdata.proms.module.pm.client.dto.StandupPolicyDto;
import com.hyperdata.proms.module.pm.client.dto.StandupReportSummaryDto;
import com.hyperdata.proms.module.pm.standup.entity.StandupReminderRuns;
import com.hyperdata.proms.module.pm.standup.entity.StandupReminderRunsId;
import com.hyperdata.proms.module.pm.standup.mapper.StandupMapper;
import com.hyperdata.proms.module.pm.standup.repository.StandupPoliciesRepository;
import com.hyperdata.proms.module.pm.standup.repository.StandupReminderRunsRepository;
import com.hyperdata.proms.module.pm.standup.repository.StandupReportRepository;
import com.hyperdata.proms.module.pm.standup.repository.spec.StandupSpecs;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandupClientImpl implements StandupClient {

    private final StandupPoliciesRepository standupPoliciesRepository;
    private final StandupReportRepository standupReportRepository;
    private final StandupReminderRunsRepository standupReminderRunsRepository;
    private final StandupMapper standupMapper;

    @Override
    public Optional<StandupPolicyDto> findPolicyByTenantId(UUID tenantId) {
        // TODO Auto-generated method stub
        return standupPoliciesRepository.findById(tenantId).map(standupMapper::toPolicyDto);
    }

    @Override
    public Optional<StandupReportSummaryDto> findUserReportOnDate(UUID tenantId, UUID userId, LocalDate localDate) {
        // TODO Auto-generated method stub
        return standupReportRepository.findOne(StandupSpecs.userReportOnDate(tenantId, userId, localDate))
            .map(standupMapper::toReportSummaryDto);
    }

    @Override
    public boolean hasReminderRun(UUID tenantId, LocalDate runDate) {
        // TODO Auto-generated method stub
        return standupReminderRunsRepository.existsById(toId(tenantId, runDate));
    }

    @Override
    @Transactional
    public void markReminderRun(UUID tenantId, LocalDate runDate) {
        // TODO Auto-generated method stub
        StandupReminderRuns run = new StandupReminderRuns();
        run.setId(toId(tenantId, runDate));
        standupReminderRunsRepository.save(run);
    }

    private StandupReminderRunsId toId(UUID tenantId, LocalDate runDate) {
        StandupReminderRunsId id = new StandupReminderRunsId();
        id.setTenantId(tenantId);
        id.setRunDate(runDate);
        return id;
    }

}
