package com.hyperdata.proms.module.work.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.work.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.work.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskSummaryDto;
import com.hyperdata.proms.module.work.client.impl.TaskClientImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskClientImplTest {

    private final TaskClientImpl taskClient = new TaskClientImpl();

    @Test
    @DisplayName("GIVEN boardId WHEN getTasksByBoard THEN returns empty PageResponse stub")
    void givenBoardId_whenGetTasksByBoard_thenReturnsEmptyPage() {
        UUID boardId = UUID.randomUUID();
        PageResponse<TaskSummaryDto> response = taskClient.getTasksByBoard(boardId,
            new BoardTaskFilterDto(null, null, null, null, null, null));
        assertThat(response).isNotNull();
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN createTaskCommand WHEN createTask THEN returns TaskCreatedDto stub")
    void givenCreateTaskCommand_whenCreateTask_thenReturnsTaskCreatedDto() {
        UUID boardId = UUID.randomUUID();
        CreateTaskCommand cmd = new CreateTaskCommand("TEST", null, UUID.randomUUID(), "Title", List.of(),
            LocalDate.now(), "high", UUID.randomUUID());
        TaskCreatedDto dto = taskClient.createTask(boardId, cmd);
        assertThat(dto).isNotNull();
        assertThat(dto.key()).isEqualTo("TEST-1");
        assertThat(dto.title()).isEqualTo("Title");
    }
}
