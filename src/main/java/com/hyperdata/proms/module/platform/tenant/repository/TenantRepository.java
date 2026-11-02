package com.hyperdata.proms.module.platform.tenant.repository;

import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantRepository extends JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {
    Optional<Tenant> findBySlug(String slug);
}
