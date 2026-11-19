package com.hyperdata.proms.module.sample.client.contract;

import com.hyperdata.proms.module.sample.client.dto.EmployeeSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeClient {

    Optional<EmployeeSummaryDto> findEmployeeSummaryById(UUID id);

    List<EmployeeSummaryDto> findAllByIds(Collection<UUID> ids);
}
