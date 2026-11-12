package com.hyperdata.proms.module.pm.standup.facade;

import com.hyperdata.proms.module.pm.standup.facade.dto.StandupPolicyDto;
import com.hyperdata.proms.module.pm.standup.facade.dto.StandupReportSummaryDto;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StandupFacade {

    Optional<StandupPolicyDto> findPolicyByTenantId(UUID tenantId);

    Optional<StandupReportSummaryDto> findUserReportOnDate(UUID tenantId, UUID userId, LocalDate reportDate);
}
