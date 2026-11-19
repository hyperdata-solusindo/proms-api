package com.hyperdata.proms.module.pm.standup.repository;

import com.hyperdata.proms.module.pm.standup.entity.StandupPolicies;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandupPoliciesRepository extends JpaRepository<StandupPolicies, UUID> {

}
