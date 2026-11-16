package com.hyperdata.proms.module.pm.client.contract;

import com.hyperdata.proms.module.pm.client.dto.StandupPolicyDto;
import com.hyperdata.proms.module.pm.client.dto.StandupReportSummaryDto;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StandupClient {

    boolean hasReminderRun(UUID tenantId, LocalDate runDate);

    Optional<StandupPolicyDto> findPolicyByTenantId(UUID tenantId);

    Optional<StandupReportSummaryDto> findUserReportOnDate(UUID tenantId, UUID userId, LocalDate localDate);

    void markReminderRun(UUID tenantId, LocalDate runDate);
}
