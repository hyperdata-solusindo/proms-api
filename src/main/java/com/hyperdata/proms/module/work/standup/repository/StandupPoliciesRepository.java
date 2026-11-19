package com.hyperdata.proms.module.work.standup.repository;

import com.hyperdata.proms.module.work.standup.entity.StandupPolicies;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandupPoliciesRepository extends JpaRepository<StandupPolicies, UUID> {

}
