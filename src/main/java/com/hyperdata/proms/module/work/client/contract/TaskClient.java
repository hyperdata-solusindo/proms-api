package com.hyperdata.proms.module.work.client.contract;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.work.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.work.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskSummaryDto;
import java.util.UUID;

public interface TaskClient {

    PageResponse<TaskSummaryDto> getTasksByBoard(UUID boardId, BoardTaskFilterDto filter);

    TaskCreatedDto createTask(UUID boardId, CreateTaskCommand command);
}
