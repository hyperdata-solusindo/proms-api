package com.hyperdata.proms.module.sample.employee.facade;

import com.hyperdata.proms.module.sample.employee.facade.dto.EmployeeSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeFacade {

    Optional<EmployeeSummaryDto> findEmployeeSummaryById(UUID id);

    List<EmployeeSummaryDto> findAllByIds(Collection<UUID> ids);
}
