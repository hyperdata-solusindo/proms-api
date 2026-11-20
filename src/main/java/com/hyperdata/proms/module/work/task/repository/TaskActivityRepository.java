package com.hyperdata.proms.module.work.task.repository;

import com.hyperdata.proms.module.work.task.entity.TaskActivities;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskActivityRepository extends JpaRepository<TaskActivities, Long> {

    @Query("""
            SELECT a FROM TaskActivities a
            WHERE a.taskId = :taskId
            ORDER BY a.id DESC
        """)
    List<TaskActivities> findLatestByTaskId(
        @Param("taskId")
        UUID taskId,
        Limit limit);

    @Query("""
            SELECT a FROM TaskActivities a
            WHERE a.taskId = :taskId
              AND a.id < :beforeId
            ORDER BY a.id DESC
        """)
    List<TaskActivities> findOlderByTaskId(
        @Param("taskId")
        UUID taskId,
        @Param("beforeId")
        Long beforeId,
        Limit limit);
}
