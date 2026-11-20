package com.hyperdata.proms.module.work.task.controller;

import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.work.task.dto.MoveTaskRequest;
import com.hyperdata.proms.module.work.task.dto.MyTaskFilter;
import com.hyperdata.proms.module.work.task.dto.MyTaskSummaryResponse;
import com.hyperdata.proms.module.work.task.dto.TaskActivityResponse;
import com.hyperdata.proms.module.work.task.dto.TaskCursorResponse;
import com.hyperdata.proms.module.work.task.dto.TaskDetailResponse;
import com.hyperdata.proms.module.work.task.dto.TaskListItemResponse;
import com.hyperdata.proms.module.work.task.dto.UpdateTaskRequest;
import com.hyperdata.proms.module.work.task.service.TaskService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TaskController implements TaskApi {

    private final TaskService taskService;

    @Override
    public RestResponse<List<TaskListItemResponse>> getMyTasks(MyTaskFilter filter) {
        MyTaskFilter.validator.validate(filter).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        TaskCursorResponse<TaskListItemResponse> page = taskService.getMyTasks(currentUser, filter);
        return RestResponse.ok(page.items(), page.meta(), Messages.get("task.my_tasks.retrieved"));
    }

    @Override
    public RestResponse<MyTaskSummaryResponse> getMyTaskSummary() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(taskService.getMyTaskSummary(currentUser), Messages.get("task.summary.retrieved"));
    }

    @Override
    public RestResponse<TaskDetailResponse> getTaskDetail(UUID taskId) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(taskService.getTaskDetail(taskId, currentUser), Messages.get("task.detail.retrieved"));
    }

    @Override
    public RestResponse<TaskDetailResponse> getTaskByKey(String taskKey) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(taskService.getTaskByKey(taskKey, currentUser), Messages.get("task.detail.retrieved"));
    }

    @Override
    public RestResponse<TaskDetailResponse> updateTask(UUID taskId, String ifMatch, UpdateTaskRequest request) {
        UpdateTaskRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(
            taskService.updateTask(taskId, currentUser, ifMatch, request),
            Messages.get("task.updated.success"));
    }

    @Override
    public RestResponse<TaskDetailResponse> moveTask(UUID taskId, String ifMatch, MoveTaskRequest request) {
        MoveTaskRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(
            taskService.moveTask(taskId, currentUser, ifMatch, request),
            Messages.get("task.moved.success"));
    }

    @Override
    public RestResponse<List<TaskActivityResponse>> getTaskActivities(UUID taskId, String cursor, Integer limit) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        TaskCursorResponse<TaskActivityResponse> page = taskService.getTaskActivities(taskId, currentUser, cursor,
            limit);
        return RestResponse.ok(page.items(), page.meta(), Messages.get("task.activities.retrieved"));
    }
}
