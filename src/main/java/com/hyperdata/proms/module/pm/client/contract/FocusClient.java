package com.hyperdata.proms.module.pm.client.contract;

import com.hyperdata.proms.module.pm.client.dto.FocusSessionSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FocusClient {

    long sumDurationSec(UUID userId, Instant from, Instant to);

    List<FocusSessionSummaryDto> findByUserInRange(UUID userId, Instant from, Instant to);
}
