package com.hyperdata.proms.module.pm.standup.repository;

import com.hyperdata.proms.module.pm.standup.entity.StandupReminderRuns;
import com.hyperdata.proms.module.pm.standup.entity.StandupReminderRunsId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandupReminderRunsRepository extends JpaRepository<StandupReminderRuns, StandupReminderRunsId> {

}
