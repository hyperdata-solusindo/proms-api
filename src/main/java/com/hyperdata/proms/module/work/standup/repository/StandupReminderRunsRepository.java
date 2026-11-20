package com.hyperdata.proms.module.work.standup.repository;

import com.hyperdata.proms.module.work.standup.entity.StandupReminderRuns;
import com.hyperdata.proms.module.work.standup.entity.StandupReminderRunsId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandupReminderRunsRepository extends JpaRepository<StandupReminderRuns, StandupReminderRunsId> {

}
