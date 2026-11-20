package com.hyperdata.proms.module.work.task.repository;

import com.hyperdata.proms.module.work.task.entity.TaskDepedencies;
import com.hyperdata.proms.module.work.task.entity.TaskDepedenciesId;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskDepedencyRepository extends JpaRepository<TaskDepedencies, TaskDepedenciesId> {

    @Query("""
            SELECT d FROM TaskDepedencies d
            WHERE d.id.blockingTaskId = :taskId
            OR d.id.blockedTaskId = :taskId
            ORDER BY d.createdAt ASC
        """)
    List<TaskDepedencies> findDependenciesOfTask(@Param("taskId")
    UUID taskId);
}
