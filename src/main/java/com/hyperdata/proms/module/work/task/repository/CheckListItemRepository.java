package com.hyperdata.proms.module.work.task.repository;

import com.hyperdata.proms.module.work.task.entity.ChecklistItem;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CheckListItemRepository extends JpaRepository<ChecklistItem, UUID> {

    @Query("""
            SELECT c FROM ChecklistItem c
            WHERE c.taskId = :taskId
            ORDER BY c.position ASC, c.createdAt ASC
        """)
    List<ChecklistItem> findChecklistByTaskId(@Param("taskId")
    UUID taskId);

    Optional<ChecklistItem> findByIdAndTaskId(UUID id, UUID taskId);

    @Query("""
            SELECT COALESCE(MAX(c.position) + 1, 0) FROM ChecklistItem c
            WHERE c.taskId = :taskId
        """)
    int findNextPosition(@Param("taskId")
    UUID taskId);

}
