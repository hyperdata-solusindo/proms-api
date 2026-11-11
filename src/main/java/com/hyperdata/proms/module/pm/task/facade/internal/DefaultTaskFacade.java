package com.hyperdata.proms.module.pm.task.facade.internal;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.task.facade.TaskFacade;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultTaskFacade implements TaskFacade {

    @Override
    public PageResponse<TaskSummaryDto> getTasksByBoard(UUID boardId, BoardTaskFilterDto filter) {
        return new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
    }

    @Override
    public TaskCreatedDto createTask(UUID boardId, CreateTaskCommand command) {
        return new TaskCreatedDto(
            UUID.randomUUID(),
            command.boardKey() + "-1",
            command.title(),
            command.columnId(),
            command.workstreamId(),
            Instant.now());
    }
}
