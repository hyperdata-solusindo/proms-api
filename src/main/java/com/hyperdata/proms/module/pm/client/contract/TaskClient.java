package com.hyperdata.proms.module.pm.client.contract;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.client.dto.TaskSummaryDto;
import java.util.UUID;

public interface TaskClient {

    PageResponse<TaskSummaryDto> getTasksByBoard(UUID boardId, BoardTaskFilterDto filter);

    TaskCreatedDto createTask(UUID boardId, CreateTaskCommand command);
}
