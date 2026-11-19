package com.hyperdata.proms.module.work.standup.facade;

import com.hyperdata.proms.module.work.standup.facade.dto.StandupPolicyDto;
import com.hyperdata.proms.module.work.standup.facade.dto.StandupReportSummaryDto;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StandupFacade {

    Optional<StandupPolicyDto> findPolicyByTenantId(UUID tenantId);

    Optional<StandupReportSummaryDto> findUserReportOnDate(UUID tenantId, UUID userId, LocalDate reportDate);
}
