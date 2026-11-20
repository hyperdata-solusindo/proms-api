package com.hyperdata.proms.module.work.task.repository;

import com.hyperdata.proms.module.work.client.dto.TaskAssigneeRefDto;
import com.hyperdata.proms.module.work.task.entity.TaskAssignees;
import com.hyperdata.proms.module.work.task.entity.TaskAssigneesId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignees, TaskAssigneesId> {

    @Query("""
            SELECT new com.hyperdata.proms.module.work.client.dto.TaskAssigneeRefDto(a.id.taskId, a.id.userId)
            FROM TaskAssignees a
            WHERE a.id.taskId IN :taskIds
            ORDER BY a.createdAt ASC
        """)
    List<TaskAssigneeRefDto> findAssigneesByTaskIds(@Param("taskIds")
    Collection<UUID> taskIds);

    @Query("""
            SELECT a.id.userId FROM TaskAssignees a
            WHERE a.id.taskId = :taskId
        """)
    List<UUID> findAssigneeUserIds(@Param("taskId")
    UUID taskId);

    @Modifying
    @Query("""
            DELETE FROM TaskAssignees a
            WHERE a.id.taskId = :taskId
              AND a.id.userId IN :userIds
        """)
    int removeAssignees(
        @Param("taskId")
        UUID taskId,
        @Param("userIds")
        Collection<UUID> userIds);

    @Modifying
    @Query("""
            DELETE FROM TaskAssignees a
            WHERE a.id.userId = :userId
              AND a.id.taskId IN (
                  SELECT t.id FROM Task t
                  WHERE t.boardId = :boardId
              )
        """)
    int unassignUserFromBoardTasks(
        @Param("boardId")
        UUID boardId,
        @Param("userId")
        UUID userId);
}
