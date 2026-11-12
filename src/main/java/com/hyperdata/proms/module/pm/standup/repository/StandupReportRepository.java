package com.hyperdata.proms.module.pm.standup.repository;

import com.hyperdata.proms.module.pm.standup.entity.StandupReports;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StandupReportRepository
    extends
        JpaRepository<StandupReports, UUID>,
        JpaSpecificationExecutor<StandupReports> {

}
