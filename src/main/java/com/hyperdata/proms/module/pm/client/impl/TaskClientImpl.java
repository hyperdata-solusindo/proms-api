package com.hyperdata.proms.module.pm.client.impl;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.client.contract.TaskClient;
import com.hyperdata.proms.module.pm.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.client.dto.TaskSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskClientImpl implements TaskClient {

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
