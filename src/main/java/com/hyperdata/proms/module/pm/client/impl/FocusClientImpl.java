package com.hyperdata.proms.module.pm.client.impl;

import com.hyperdata.proms.module.pm.client.contract.FocusClient;
import com.hyperdata.proms.module.pm.client.dto.FocusSessionSummaryDto;
import com.hyperdata.proms.module.pm.focus.repository.FocusSessionRepository;
import com.hyperdata.proms.module.pm.focus.repository.spec.FocusSpecs;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusClientImpl implements FocusClient {

    private final FocusSessionRepository focusSessionRepository;
    @Override
    public List<FocusSessionSummaryDto> findByUserInRange(UUID userId, Instant from, Instant to) {
        // TODO Auto-generated method stub
        return focusSessionRepository
            .findAll(
                FocusSpecs.userSessionsInRange(null, userId, from, to),
                Sort.by(Sort.Direction.DESC, "startedAt"))
            .stream()
            .map(f -> new FocusSessionSummaryDto(
                f.getId(), f.getUserId(), f.getStartedAt(), f.getEndedAt(), f.getDurationSec()))
            .toList();
    }

    @Override
    public long sumDurationSec(UUID userId, Instant from, Instant to) {
        // TODO Auto-generated method stub
        return focusSessionRepository.sumDurationSecInRange(userId, from, to);
    }

}
