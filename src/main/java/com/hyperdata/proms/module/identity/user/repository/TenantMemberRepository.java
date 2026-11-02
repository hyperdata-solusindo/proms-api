package com.hyperdata.proms.module.identity.user.repository;

import com.hyperdata.proms.module.identity.user.entity.TenantMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantMemberRepository
        extends JpaRepository<TenantMember, UUID>, JpaSpecificationExecutor<TenantMember> {

    // Level 1: Derived query (2 simple fields)
    List<TenantMember> findByUserIdAndStatus(UUID userId, String status);

    // Level 2: Explicit @Query JPQL (> 2 parameters & clear business meaning)
    @Query(
            """
                SELECT tm FROM TenantMember tm
                WHERE tm.userId = :userId
                  AND tm.tenantId = :tenantId
                  AND tm.status = :status
            """)
    Optional<TenantMember> findMembership(
            @Param("userId") UUID userId, @Param("tenantId") UUID tenantId, @Param("status") String status);
}
