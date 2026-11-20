package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.WorkflowTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    @Query("""
            SELECT wt FROM WorkflowTemplate wt
            LEFT JOIN FETCH wt.columns
            WHERE wt.id = :id AND (wt.tenantId IS NULL OR wt.tenantId = :tenantId)
        """)
    Optional<WorkflowTemplate> findByIdAndTenant(@Param("id")
    UUID id, @Param("tenantId")
    UUID tenantId);

    @Query("""
            SELECT wt FROM WorkflowTemplate wt
            LEFT JOIN FETCH wt.columns
            WHERE wt.tenantId IS NULL
            ORDER BY wt.createdAt ASC
            LIMIT 1
        """)
    Optional<WorkflowTemplate> findGlobalDefaultTemplate();

    @Query("""
            SELECT DISTINCT wt FROM WorkflowTemplate wt
            LEFT JOIN FETCH wt.columns
            WHERE wt.tenantId IS NULL OR wt.tenantId = :tenantId
            ORDER BY wt.createdAt ASC
        """)
    List<WorkflowTemplate> findAllAvailable(@Param("tenantId")
    UUID tenantId);
}
