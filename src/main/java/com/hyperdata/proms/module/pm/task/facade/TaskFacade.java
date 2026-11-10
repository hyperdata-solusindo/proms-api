package com.hyperdata.proms.module.pm.task.facade;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import java.util.UUID;

public interface TaskFacade {

    PageResponse<TaskSummaryDto> getTasksByBoard(UUID boardId, BoardTaskFilterDto filter);

    TaskCreatedDto createTask(UUID boardId, CreateTaskCommand command);
}
