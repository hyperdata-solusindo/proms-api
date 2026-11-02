package com.hyperdata.proms.module.platform.tenant.repository;

import com.hyperdata.proms.module.platform.tenant.entity.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {

    @Query(
            """
                SELECT s FROM Subscription s
                WHERE s.tenantId = :tenantId
                  AND s.status = :status
                ORDER BY s.createdAt DESC
                LIMIT 1
            """)
    Optional<Subscription> findLatestActiveByTenantId(@Param("tenantId") UUID tenantId, @Param("status") String status);
}
