package com.hyperdata.proms.module.pm.standup.facade.internal;

import com.hyperdata.proms.module.pm.standup.facade.StandupFacade;
import com.hyperdata.proms.module.pm.standup.facade.dto.StandupPolicyDto;
import com.hyperdata.proms.module.pm.standup.facade.dto.StandupReportSummaryDto;
import com.hyperdata.proms.module.pm.standup.mapper.StandupMapper;
import com.hyperdata.proms.module.pm.standup.repository.StandupPoliciesRepository;
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
public class DefaultStandupFacade implements StandupFacade {

    private final StandupPoliciesRepository policiesRepository;
    private final StandupReportRepository reportsRepository;
    private final StandupMapper standupMapper;

    @Override
    public Optional<StandupPolicyDto> findPolicyByTenantId(UUID tenantId) {
        return policiesRepository.findById(tenantId).map(standupMapper::toPolicyDto);
    }

    @Override
    public Optional<StandupReportSummaryDto> findUserReportOnDate(UUID tenantId, UUID userId, LocalDate reportDate) {
        return reportsRepository.findOne(StandupSpecs.userReportOnDate(tenantId, userId, reportDate))
            .map(standupMapper::toReportSummaryDto);
    }
}
