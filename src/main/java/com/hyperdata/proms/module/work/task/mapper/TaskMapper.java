package com.hyperdata.proms.module.work.task.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.module.work.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskSubtaskCountDto;
import com.hyperdata.proms.module.work.client.dto.TaskUserDto;
import com.hyperdata.proms.module.work.task.constant.TaskCategory;
import com.hyperdata.proms.module.work.task.constant.TaskDueDate;
import com.hyperdata.proms.module.work.task.constant.TaskKeyFormat;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import com.hyperdata.proms.module.work.task.dto.CreateTaskRequest;
import com.hyperdata.proms.module.work.task.dto.TaskActivityResponse;
import com.hyperdata.proms.module.work.task.dto.TaskDetailResponse;
import com.hyperdata.proms.module.work.task.dto.TaskListItemResponse;
import com.hyperdata.proms.module.work.task.dto.UpdateTaskRequest;
import com.hyperdata.proms.module.work.task.entity.ChecklistItem;
import com.hyperdata.proms.module.work.task.entity.Task;
import com.hyperdata.proms.module.work.task.entity.TaskActivities;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    ObjectMapper JSON = new ObjectMapper();
    TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {
    };

    @Mapping(target = "description", source = "description", qualifiedByName = "mapToJson")
    Task toEntity(CreateTaskRequest request);

    @Mapping(target = "createdBy", source = "creatorId")
    Task toEntity(CreateTaskCommand command);

    Task toSubtaskEntity(CreateTaskRequest.SubtaskInput input);

    @Mapping(target = "title", source = "text")
    @Mapping(target = "done", source = "done", defaultValue = "false")
    ChecklistItem toChecklistEntity(CreateTaskRequest.ChecklistItemInput input);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "description", source = "description", qualifiedByName = "mapToJson")
    void updateEntity(UpdateTaskRequest request, @MappingTarget
    Task task);

    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "isBlocked", source = "task.blocked")
    @Mapping(target = "dueState", expression = "java(toDueState(task.getDueDate(), category, today))")
    @Mapping(target = "daysLate", expression = "java(toDaysLate(task.getDueDate(), category, today))")
    @Mapping(target = "subtaskDone", source = "subtaskCount.done")
    @Mapping(target = "subtaskTotal", source = "subtaskCount.total")
    @Mapping(target = "version", source = "task.updatedAt")
    TaskListItemResponse toListItemResponse(Task task, String key, String boardName, String category,
        LocalDate today, List<TaskUserDto> assignees, TaskSubtaskCountDto subtaskCount, int commentCount);

    /** Task lengkap untuk API-15/16/17/18/19. */
    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "description", source = "task.description", qualifiedByName = "jsonToMap")
    @Mapping(target = "isBlocked", source = "task.blocked")
    @Mapping(target = "dueState", expression = "java(toDueState(task.getDueDate(), category, today))")
    @Mapping(target = "daysLate", expression = "java(toDaysLate(task.getDueDate(), category, today))")
    @Mapping(target = "createdBy", source = "creator")
    @Mapping(target = "createdAt", source = "task.createdAt")
    @Mapping(target = "version", source = "task.updatedAt")
    TaskDetailResponse toDetailResponse(Task task, String key, String boardName, String category, LocalDate today,
        List<TaskUserDto> assignees, List<TaskDetailResponse.SubtaskResponse> subtasks,
        List<TaskDetailResponse.ChecklistItemResponse> checklist, TaskDetailResponse.DependenciesResponse dependencies,
        List<TaskDetailResponse.AttachmentResponse> attachments, TaskDetailResponse.TaskPermissionResponse permissions,
        TaskUserDto creator);

    /** Subtask dianggap selesai bila {@code completedAt} terisi (diisi trigger saat masuk kolom done). */
    @Mapping(target = "id", source = "subtask.id")
    @Mapping(target = "title", source = "subtask.title")
    @Mapping(target = "isDone", expression = "java(subtask.getCompletedAt() != null)")
    TaskDetailResponse.SubtaskResponse toSubtaskResponse(Task subtask, String key, String category);

    @Mapping(target = "isDone", source = "done")
    TaskDetailResponse.ChecklistItemResponse toChecklistItemResponse(ChecklistItem item);

    List<TaskDetailResponse.ChecklistItemResponse> toChecklistItemResponses(List<ChecklistItem> items);

    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "title", source = "task.title")
    TaskDetailResponse.TaskRefResponse toTaskRefResponse(Task task, String key, String category);

    /** API-22. {@code actor} null bila aksi dilakukan sistem. */
    @Mapping(target = "id", source = "activity.id")
    @Mapping(target = "oldValue", source = "activity.oldValue", qualifiedByName = "jsonToValue")
    @Mapping(target = "newValue", source = "activity.newValue", qualifiedByName = "jsonToValue")
    @Mapping(target = "createdAt", source = "activity.createdAt")
    TaskActivityResponse toActivityResponse(TaskActivities activity, TaskUserDto actor);

    /** Hasil TaskClient#createTask untuk modul board. */
    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "title", source = "task.title")
    @Mapping(target = "version", source = "task.updatedAt")
    TaskCreatedDto toCreatedDto(Task task, String key);

    default String toKey(String boardKey, Integer number) {
        return boardKey + TaskKeyFormat.SEPARATOR + number;
    }

    default String toDueState(LocalDate dueDate, String category, LocalDate today) {
        if (dueDate == null) {
            return TaskDueDate.NONE;
        }
        if (TaskCategory.DONE.equals(category)) {
            return TaskDueDate.OK;
        }
        if (dueDate.isBefore(today)) {
            return TaskDueDate.LATE;
        }
        if (!dueDate.isAfter(today.plusDays(TaskLimits.DUE_SOON_DAYS))) {
            return TaskDueDate.SOON;
        }
        return TaskDueDate.OK;
    }

    /** Jumlah hari terlambat; 0 bila tidak terlambat, tanpa due date, atau sudah done. */
    default int toDaysLate(LocalDate dueDate, String category, LocalDate today) {
        if (dueDate == null || TaskCategory.DONE.equals(category) || !dueDate.isBefore(today)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(dueDate, today);
    }

    /** Dokumen editor (Map) → String JSON untuk kolom {@code tasks.description}. */
    @Named("mapToJson")
    default String mapToJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Dokumen deskripsi task tidak dapat diserialisasi", e);
        }
    }

    /** String JSON objek → Map (deskripsi task). */
    @Named("jsonToMap")
    default Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, JSON_OBJECT);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kolom JSON task berisi data tidak valid", e);
        }
    }

    /** String JSON bebas (objek, array, string, angka) → nilai Java, untuk old/new value activity. */
    @Named("jsonToValue")
    default Object jsonToValue(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kolom JSON activity berisi data tidak valid", e);
        }
    }
}
