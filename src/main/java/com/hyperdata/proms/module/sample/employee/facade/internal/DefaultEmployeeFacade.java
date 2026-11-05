package com.hyperdata.proms.module.sample.employee.facade.internal;

import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.module.sample.employee.facade.EmployeeFacade;
import com.hyperdata.proms.module.sample.employee.facade.dto.EmployeeSummaryDto;
import com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapper;
import com.hyperdata.proms.module.sample.employee.repository.EmployeeRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DefaultEmployeeFacade implements EmployeeFacade {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeSummaryDto> findEmployeeSummaryById(UUID id) {
        return employeeRepository.findById(id).map(employeeMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSummaryDto> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return Lookups.map(employeeRepository.findAllById(ids), employeeMapper::toSummaryDto);
    }
}
