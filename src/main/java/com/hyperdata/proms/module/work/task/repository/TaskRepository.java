package com.hyperdata.proms.module.work.task.repository;

import com.hyperdata.proms.module.work.client.dto.TaskBoardColumnCountDto;
import com.hyperdata.proms.module.work.client.dto.TaskColumnCountDto;
import com.hyperdata.proms.module.work.client.dto.TaskSubtaskCountDto;
import com.hyperdata.proms.module.work.task.entity.Task;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    @Query("""
            SELECT t FROM Task t
            WHERE t.id = :id
              AND t.deletedAt IS NULL
        """)
    Optional<Task> findActiveById(@Param("id")
    UUID id);

    @Query("""
            SELECT t FROM Task t
            WHERE t.boardId = :boardId
              AND t.number = :number
              AND t.deletedAt IS NULL
        """)
    Optional<Task> findActiveByBoardAndNumber(
        @Param("boardId")
        UUID boardId,
        @Param("number")
        Integer number);

    @Query("""
            SELECT t FROM Task t
            WHERE t.id IN :ids
              AND t.deletedAt IS NULL
        """)
    List<Task> findActiveByIds(@Param("ids")
    Collection<UUID> ids);

    @Query("""
            SELECT t FROM Task t
            WHERE t.parentId = :parentId
              AND t.deletedAt IS NULL
            ORDER BY t.position ASC, t.createdAt ASC
        """)
    List<Task> findActiveSubtasksByParentId(@Param("parentId")
    UUID parentId);

    @Query("""
            SELECT new com.hyperdata.proms.module.work.client.dto.TaskSubtaskCountDto(
                t.parentId,
                SUM(CASE WHEN t.completedAt IS NOT NULL THEN 1L ELSE 0L END),
                COUNT(t.id))
            FROM Task t
            WHERE t.parentId IN :parentIds
              AND t.deletedAt IS NULL
            GROUP BY t.parentId
        """)
    List<TaskSubtaskCountDto> countSubtasksGroupedByParentIds(@Param("parentIds")
    Collection<UUID> parentIds);

    @Query("""
            SELECT new com.hyperdata.proms.module.work.client.dto.TaskColumnCountDto(t.columnId, COUNT(t.id))
            FROM Task t
            WHERE t.boardId = :boardId
              AND t.deletedAt IS NULL
            GROUP BY t.columnId
        """)
    List<TaskColumnCountDto> countActiveTasksGroupedByColumn(@Param("boardId")
    UUID boardId);

    @Query("""
            SELECT new com.hyperdata.proms.module.work.client.dto.TaskBoardColumnCountDto(
                t.boardId, t.columnId, COUNT(t.id))
            FROM Task t
            WHERE t.boardId IN :boardIds
              AND t.deletedAt IS NULL
            GROUP BY t.boardId, t.columnId
        """)
    List<TaskBoardColumnCountDto> countActiveTasksGroupedByBoardAndColumn(@Param("boardIds")
    Collection<UUID> boardIds);

    @Query("""
            SELECT COUNT(t.id) FROM Task t
            WHERE t.columnId = :columnId
              AND t.deletedAt IS NULL
        """)
    long countActiveTasksInColumn(@Param("columnId")
    UUID columnId);

    @Query("""
            SELECT MAX(t.position) FROM Task t
            WHERE t.columnId = :columnId
              AND t.deletedAt IS NULL
        """)
    Optional<String> findLastPositionInColumn(@Param("columnId")
    UUID columnId);

    @Query("""
            SELECT DISTINCT t.boardId FROM Task t
            WHERE t.deletedAt IS NULL
              AND t.id IN (
                  SELECT a.id.taskId FROM TaskAssignees a
                  WHERE a.id.userId = :userId
              )
        """)
    List<UUID> findBoardIdsAssignedTo(@Param("userId")
    UUID userId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.number = :number
              AND t.deletedAt IS NULL
        """)
    List<Task> findActiveByNumber(@Param("number")
    Integer number);
}
