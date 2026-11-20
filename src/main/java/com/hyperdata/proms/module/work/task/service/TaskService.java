package com.hyperdata.proms.module.work.task.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.TenantScoped;
import com.hyperdata.proms.common.util.ConcurrencyGuard;
import com.hyperdata.proms.common.util.FractionalIndex;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.constant.BoardStatus;
import com.hyperdata.proms.module.work.client.contract.BoardClient;
import com.hyperdata.proms.module.work.client.dto.BoardPermissionDto;
import com.hyperdata.proms.module.work.client.dto.BoardSummaryDto;
import com.hyperdata.proms.module.work.client.dto.TaskAssigneeRefDto;
import com.hyperdata.proms.module.work.client.dto.TaskSubtaskCountDto;
import com.hyperdata.proms.module.work.client.dto.TaskUserDto;
import com.hyperdata.proms.module.work.task.constant.TaskActivityAction;
import com.hyperdata.proms.module.work.task.constant.TaskActivityField;
import com.hyperdata.proms.module.work.task.constant.TaskCategory;
import com.hyperdata.proms.module.work.task.constant.TaskDueDate;
import com.hyperdata.proms.module.work.task.constant.TaskDueFilter;
import com.hyperdata.proms.module.work.task.constant.TaskErrorCode;
import com.hyperdata.proms.module.work.task.constant.TaskKeyFormat;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import com.hyperdata.proms.module.work.task.constant.TaskStatusFilter;
import com.hyperdata.proms.module.work.task.dto.MoveTaskRequest;
import com.hyperdata.proms.module.work.task.dto.MyTaskFilter;
import com.hyperdata.proms.module.work.task.dto.MyTaskSummaryResponse;
import com.hyperdata.proms.module.work.task.dto.TaskActivityResponse;
import com.hyperdata.proms.module.work.task.dto.TaskCursorResponse;
import com.hyperdata.proms.module.work.task.dto.TaskDetailResponse;
import com.hyperdata.proms.module.work.task.dto.TaskListItemResponse;

import com.hyperdata.proms.module.work.task.dto.UpdateTaskRequest;
import com.hyperdata.proms.module.work.task.entity.ChecklistItem;
import com.hyperdata.proms.module.work.task.entity.Task;
import com.hyperdata.proms.module.work.task.entity.TaskActivities;
import com.hyperdata.proms.module.work.task.entity.TaskAssignees;
import com.hyperdata.proms.module.work.task.entity.TaskAssigneesId;
import com.hyperdata.proms.module.work.task.entity.TaskDepedencies;
import com.hyperdata.proms.module.work.task.mapper.TaskMapper;
import com.hyperdata.proms.module.work.task.repository.CheckListItemRepository;
import com.hyperdata.proms.module.work.task.repository.TaskActivityRepository;
import com.hyperdata.proms.module.work.task.repository.TaskAssigneeRepository;
import com.hyperdata.proms.module.work.task.repository.TaskDepedencyRepository;
import com.hyperdata.proms.module.work.task.repository.TaskRepository;
import com.hyperdata.proms.module.work.task.repository.spec.TaskSpec;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logika bisnis fitur task untuk mobile v1: API-04 (My Tasks) + ringkasan Home, API-15/16 (detail), API-17 (PATCH),
 * API-18 (move), dan API-22 (activities). API-14 dan API-19 dilayani {@code BoardService} lewat {@code TaskClient}.
 *
 * <p>
 * Data board (key, nama, kategori kolom, hak akses) diambil lewat {@link BoardClient}, profil user lewat
 * {@link UserClient}, dan zona waktu tenant lewat {@link TenantClient} (Golden Stack, tanpa JOIN lintas fitur).
 */
@Slf4j
@Service
@TenantScoped
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private static final Pattern CURSOR_OFFSET = Pattern.compile("\"o\"\\s*:\\s*(\\d+)");
    private static final Pattern CURSOR_BEFORE_ID = Pattern.compile("\"b\"\\s*:\\s*(\\d+)");
    private static final String CURSOR_OFFSET_TEMPLATE = "{\"o\":%d}";
    private static final String CURSOR_BEFORE_ID_TEMPLATE = "{\"b\":%d}";
    private static final String ACTIVITY_VALUE_KEY = "value";
    private static final int INITIALS_SINGLE_WORD_LENGTH = 2;

    /** Kategori kolom yang termasuk status {@code to_do} pada filter My Tasks. */
    private static final Set<String> TODO_CATEGORIES = Set.of(
        TaskCategory.NOT_STARTED,
        TaskCategory.TO_DO,
        TaskCategory.BACKLOG,
        TaskCategory.READY);

    private static final Set<String> READ_ONLY_ROLES = Set.of(BoardMemberRole.VIEWER, BoardMemberRole.GUEST);

    private static final Set<String> DONE_MOVER_ROLES = Set.of(
        BoardMemberRole.OWNER,
        BoardMemberRole.ADMIN,
        BoardMemberRole.PM);

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final CheckListItemRepository checklistItemRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final TaskDepedencyRepository taskDependencyRepository;
    private final TaskMapper taskMapper;
    private final BoardClient boardClient;
    private final UserClient userClient;
    private final TenantClient tenantClient;
    private final EntityManager entityManager;

    /** API-04 {@code GET /me/tasks}: task yang di-assign ke user aktif, urut due date terdekat. */
    public TaskCursorResponse<TaskListItemResponse> getMyTasks(CurrentUser currentUser, MyTaskFilter filter) {
        UUID userId = currentUser.userId();
        List<UUID> boardIds = resolveMyTaskBoardIds(userId, filter.boardId());
        if (boardIds.isEmpty()) {
            return new TaskCursorResponse<>(List.of(), null);
        }

        Map<UUID, String> columnCategories = boardClient.getColumnCategories(boardIds);
        LocalDate today = tenantToday(currentUser.activeTenantId());
        Specification<Task> spec = myTaskSpec(userId, boardIds, filter, columnCategories, today);

        int limit = resolveLimit(filter.limit());
        int offset = decodeCursorNumber(filter.cursor(), CURSOR_OFFSET).map(Long::intValue).orElse(0);
        PageRequest pageable = PageRequest.of(
            offset / limit,
            limit,
            Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("createdAt")));
        Page<Task> page = taskRepository.findAll(spec, pageable);

        List<TaskListItemResponse> items = toListItems(page.getContent(), columnCategories, today);
        String nextCursor = page.hasNext()
            ? encodeCursor(CURSOR_OFFSET_TEMPLATE, (long) offset + items.size())
            : null;
        return new TaskCursorResponse<>(items, nextCursor);
    }

    /** Kartu Home: jumlah task aktif saya per segmen jatuh tempo (task di kolom done tidak dihitung). */
    public MyTaskSummaryResponse getMyTaskSummary(CurrentUser currentUser) {
        UUID userId = currentUser.userId();
        List<UUID> boardIds = taskRepository.findBoardIdsAssignedTo(userId);
        if (boardIds.isEmpty()) {
            return new MyTaskSummaryResponse(0, 0, 0, 0);
        }

        Map<UUID, String> columnCategories = boardClient.getColumnCategories(boardIds);
        LocalDate today = tenantToday(currentUser.activeTenantId());
        Specification<Task> base = TaskSpec.isActive()
            .and(TaskSpec.assignedTo(userId))
            .and(TaskSpec.inBoards(boardIds))
            .and(TaskSpec.notInColumns(columnsWhere(columnCategories, TaskCategory.DONE::equals)));

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new MyTaskSummaryResponse(
            taskRepository.count(base.and(TaskSpec.dueOn(today))),
            taskRepository.count(base.and(TaskSpec.dueBefore(today))),
            taskRepository.count(base.and(TaskSpec.dueBetween(weekStart, weekEnd))),
            taskRepository.count(base));
    }

    // ===================== API-15 / API-16 Detail =====================

    /** API-15 {@code GET /tasks/{taskId}}. */
    public TaskDetailResponse getTaskDetail(UUID taskId, CurrentUser currentUser) {
        Task task = findActiveTask(taskId);
        BoardPermissionDto permission = resolveReadPermission(task, currentUser.userId());
        return buildDetail(task, permission);
    }

    /** API-16 {@code GET /task-keys/{taskKey}}: resolve {@code NB-12} ke task. Key tidak valid → 404. */
    public TaskDetailResponse getTaskByKey(String taskKey, CurrentUser currentUser) {
        Matcher matcher = TaskKeyFormat.PATTERN.matcher(taskKey == null ? "" : taskKey.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw taskNotFound();
        }
        String boardKey = matcher.group(1);
        int number = Integer.parseInt(matcher.group(2));

        List<Task> candidates = taskRepository.findActiveByNumber(number);
        Map<UUID, BoardSummaryDto> boards = loadBoards(Lookups.extractIds(candidates, Task::getBoardId));
        Task task = candidates.stream()
            .filter(t -> {
                BoardSummaryDto board = boards.get(t.getBoardId());
                return board != null && boardKey.equals(board.key());
            })
            .findFirst()
            .orElseThrow(TaskService::taskNotFound);

        BoardPermissionDto permission = resolveReadPermission(task, currentUser.userId());
        return buildDetail(task, permission);
    }

    @Transactional
    public TaskDetailResponse updateTask(UUID taskId, CurrentUser currentUser, String ifMatch,
        UpdateTaskRequest request) {
        UUID userId = currentUser.userId();
        Task task = findActiveTask(taskId);
        BoardPermissionDto permission = resolveReadPermission(task, userId);
        requireWritable(permission, TaskErrorCode.FORBIDDEN_TASK_EDIT, "task.error.forbidden_edit");
        ConcurrencyGuard.requireMatch(ifMatch, task.getUpdatedAt(), () -> buildDetail(task, permission));

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();
        String oldPriority = task.getPriority();
        Object oldStartDate = task.getStartDate();
        Object oldDueDate = task.getDueDate();
        Object oldMilestoneId = task.getMilestoneId();

        taskMapper.updateEntity(request, task);
        if (request.title() != null) {
            task.setTitle(request.title().trim());
        }

        List<TaskActivities> activities = new ArrayList<>();
        addFieldActivity(activities, task, userId, TaskActivityField.TITLE, oldTitle, task.getTitle());
        addFieldActivity(activities, task, userId, TaskActivityField.PRIORITY, oldPriority, task.getPriority());
        addFieldActivity(activities, task, userId, TaskActivityField.START_DATE, oldStartDate, task.getStartDate());
        addFieldActivity(activities, task, userId, TaskActivityField.DUE_DATE, oldDueDate, task.getDueDate());
        addFieldActivity(activities, task, userId, TaskActivityField.MILESTONE, oldMilestoneId,
            task.getMilestoneId());
        if (!Objects.equals(oldDescription, task.getDescription())) {
            // isi dokumen deskripsi bisa besar; cukup catat bahwa deskripsi berubah
            activities.add(newActivity(task, userId, TaskActivityField.DESCRIPTION, null, null));
        }
        if (request.assigneeIds() != null) {
            syncAssignees(task, userId, request.assigneeIds(), activities);
        }

        taskRepository.saveAndFlush(task);
        entityManager.refresh(task);
        taskActivityRepository.saveAll(activities);

        log.info("Task diperbarui: taskId={}, tenantId={}, userId={}, perubahan={}",
            taskId, currentUser.activeTenantId(), userId, activities.size());
        return buildDetail(task, permission);
    }

    @Transactional
    public TaskDetailResponse moveTask(UUID taskId, CurrentUser currentUser, String ifMatch,
        MoveTaskRequest request) {
        UUID userId = currentUser.userId();
        Task task = findActiveTask(taskId);
        BoardPermissionDto permission = resolveReadPermission(task, userId);
        requireWritable(permission, CommonErrorCode.BR_WF_06, "task.error.forbidden_move");
        ConcurrencyGuard.requireMatch(ifMatch, task.getUpdatedAt(), () -> buildDetail(task, permission));

        Map<UUID, String> columnCategories = boardClient.getColumnCategories(Set.of(task.getBoardId()));
        String targetCategory = columnCategories.get(request.columnId());
        if (targetCategory == null) {
            throw new ResourceNotFoundException(TaskErrorCode.COLUMN_NOT_FOUND,
                Messages.get("task.error.column_not_found"));
        }
        if (TaskCategory.DONE.equals(targetCategory) && !canMoveToDone(permission)) {
            throw new BusinessException(CommonErrorCode.BR_WF_07, Messages.get("task.error.move_to_done_denied"),
                HttpStatus.FORBIDDEN);
        }
        boolean toBlocked = TaskCategory.BLOCKED.equals(targetCategory);
        if (toBlocked && (request.blockedReason() == null || request.blockedReason().isBlank())) {
            throw new BusinessException(CommonErrorCode.BR_WF_09,
                Messages.get("task.error.blocked_reason_required"));
        }
        if (request.columnId().equals(task.getColumnId())) {
            return buildDetail(task, permission);
        }

        UUID oldColumnId = task.getColumnId();
        task.setColumnId(request.columnId());
        task.setPosition(FractionalIndex.keyAfter(
            taskRepository.findLastPositionInColumn(request.columnId()).orElse(null)));
        task.setBlocked(toBlocked);
        task.setBlockedReason(toBlocked ? request.blockedReason().trim() : null);

        taskRepository.saveAndFlush(task);
        // completed_at diisi trigger saat masuk/keluar kolom done; updated_at untuk version
        entityManager.refresh(task);
        taskActivityRepository.save(
            newActivity(task, userId, TaskActivityField.COLUMN, oldColumnId, request.columnId()));

        log.info("Task dipindah: taskId={}, tenantId={}, userId={}, dari={}, ke={}",
            taskId, currentUser.activeTenantId(), userId, oldColumnId, request.columnId());
        return buildDetail(task, permission);
    }

    public TaskCursorResponse<TaskActivityResponse> getTaskActivities(UUID taskId, CurrentUser currentUser,
        String cursor, Integer limit) {
        Task task = findActiveTask(taskId);
        resolveReadPermission(task, currentUser.userId());

        int size = resolveLimit(limit);
        Limit fetchLimit = Limit.of(size + 1);
        Optional<Long> beforeId = decodeCursorNumber(cursor, CURSOR_BEFORE_ID);
        List<TaskActivities> rows = beforeId
            .map(id -> taskActivityRepository.findOlderByTaskId(taskId, id, fetchLimit))
            .orElseGet(() -> taskActivityRepository.findLatestByTaskId(taskId, fetchLimit));

        boolean hasMore = rows.size() > size;
        List<TaskActivities> pageRows = hasMore ? rows.subList(0, size) : rows;
        Map<UUID, TaskUserDto> actors = loadUsers(Lookups.extractIds(pageRows, TaskActivities::getActorId));
        List<TaskActivityResponse> items = Lookups.map(
            pageRows,
            activity -> taskMapper.toActivityResponse(activity, actors.get(activity.getActorId())));

        String nextCursor = hasMore
            ? encodeCursor(CURSOR_BEFORE_ID_TEMPLATE, pageRows.get(pageRows.size() - 1).getId())
            : null;
        return new TaskCursorResponse<>(items, nextCursor);
    }

    private Task findActiveTask(UUID taskId) {
        return taskRepository.findActiveById(taskId).orElseThrow(TaskService::taskNotFound);
    }

    /**
     * Bukan anggota board → 404 (keberadaan task tidak dibocorkan). Guest hanya boleh membaca task yang
     * {@code shared_with_client}.
     */
    private BoardPermissionDto resolveReadPermission(Task task, UUID userId) {
        BoardPermissionDto permission = boardClient.getPermission(task.getBoardId(), userId)
            .orElseThrow(TaskService::taskNotFound);
        if (BoardMemberRole.GUEST.equals(permission.role()) && !task.isSharedWithClient()) {
            throw taskNotFound();
        }
        return permission;
    }

    /** Board diarsipkan → 409 BOARD_ARCHIVED; viewer/guest → 403 dengan kode sesuai aksi. */
    private static void requireWritable(BoardPermissionDto permission, String forbiddenCode, String messageKey) {
        if (BoardStatus.ARCHIVED.equals(permission.boardStatus())) {
            throw new BusinessException(CommonErrorCode.BOARD_ARCHIVED, Messages.get("task.error.board_archived"),
                HttpStatus.CONFLICT);
        }
        if (READ_ONLY_ROLES.contains(permission.role())) {
            throw new BusinessException(forbiddenCode, Messages.get(messageKey), HttpStatus.FORBIDDEN);
        }
    }

    private static boolean canEdit(BoardPermissionDto permission) {
        return !BoardStatus.ARCHIVED.equals(permission.boardStatus())
            && !READ_ONLY_ROLES.contains(permission.role());
    }

    private static boolean canMoveToDone(BoardPermissionDto permission) {
        return canEdit(permission) && (permission.isOwner() || DONE_MOVER_ROLES.contains(permission.role()));
    }

    private static TaskDetailResponse.TaskPermissionResponse toPermissionResponse(BoardPermissionDto permission) {
        boolean canEdit = canEdit(permission);
        return new TaskDetailResponse.TaskPermissionResponse(canEdit, canMoveToDone(permission), canEdit);
    }

    private TaskDetailResponse buildDetail(Task task, BoardPermissionDto permission) {
        UUID taskId = task.getId();
        LocalDate today = tenantToday(task.getTenantId());

        List<Task> subtasks = taskRepository.findActiveSubtasksByParentId(taskId);
        List<ChecklistItem> checklist = checklistItemRepository.findChecklistByTaskId(taskId);
        List<TaskDepedencies> dependencies = taskDependencyRepository.findDependenciesOfTask(taskId);
        Set<UUID> relatedIds = new LinkedHashSet<>();
        for (TaskDepedencies dependency : dependencies) {
            relatedIds.add(otherTaskId(dependency, taskId));
        }
        Map<UUID, Task> relatedTasks = relatedIds.isEmpty()
            ? Map.of()
            : Lookups.toMap(taskRepository.findActiveByIds(relatedIds), Task::getId);

        Set<UUID> boardIds = Lookups.combineIds(
            Set.of(task.getBoardId()),
            Lookups.extractIds(relatedTasks.values(), Task::getBoardId));
        Map<UUID, BoardSummaryDto> boards = loadBoards(boardIds);
        Map<UUID, String> columnCategories = boardClient.getColumnCategories(boardIds);

        List<UUID> assigneeIds = taskAssigneeRepository.findAssigneeUserIds(taskId);
        Set<UUID> userIds = Lookups.combineIds(
            assigneeIds,
            task.getCreatedBy() == null ? Set.of() : Set.of(task.getCreatedBy()));
        Map<UUID, TaskUserDto> users = loadUsers(userIds);

        List<TaskDetailResponse.SubtaskResponse> subtaskResponses = Lookups.map(subtasks, subtask -> taskMapper
            .toSubtaskResponse(subtask, taskKey(boards, subtask), columnCategories.get(subtask.getColumnId())));

        List<TaskDetailResponse.TaskRefResponse> blockedBy = new ArrayList<>();
        List<TaskDetailResponse.TaskRefResponse> blocking = new ArrayList<>();
        for (TaskDepedencies dependency : dependencies) {
            Task other = relatedTasks.get(otherTaskId(dependency, taskId));
            if (other == null) {
                continue;
            }
            TaskDetailResponse.TaskRefResponse ref = taskMapper.toTaskRefResponse(
                other, taskKey(boards, other), columnCategories.get(other.getColumnId()));
            if (taskId.equals(dependency.getId().getBlockedTaskId())) {
                blockedBy.add(ref);
            } else {
                blocking.add(ref);
            }
        }

        BoardSummaryDto board = boards.get(task.getBoardId());
        return taskMapper.toDetailResponse(
            task,
            taskKey(boards, task),
            board != null ? board.name() : null,
            columnCategories.get(task.getColumnId()),
            today,
            Lookups.map(assigneeIds, users::get).stream().filter(Objects::nonNull).toList(),
            subtaskResponses,
            taskMapper.toChecklistItemResponses(checklist),
            new TaskDetailResponse.DependenciesResponse(blockedBy, blocking),
            // lampiran dimiliki modul attachment yang belum tersedia
            List.of(),
            toPermissionResponse(permission),
            task.getCreatedBy() == null ? null : users.get(task.getCreatedBy()));
    }

    private List<TaskListItemResponse> toListItems(List<Task> tasks, Map<UUID, String> columnCategories,
        LocalDate today) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Set<UUID> taskIds = Lookups.extractIds(tasks, Task::getId);
        Map<UUID, BoardSummaryDto> boards = loadBoards(Lookups.extractIds(tasks, Task::getBoardId));
        Map<UUID, TaskSubtaskCountDto> subtaskCounts = Lookups.toMap(
            taskRepository.countSubtasksGroupedByParentIds(taskIds),
            TaskSubtaskCountDto::parentId);

        List<TaskAssigneeRefDto> assigneeRefs = taskAssigneeRepository.findAssigneesByTaskIds(taskIds);
        Map<UUID, TaskUserDto> users = loadUsers(Lookups.extractIds(assigneeRefs, TaskAssigneeRefDto::userId));
        Map<UUID, List<TaskUserDto>> assigneesByTask = Lookups.groupingBy(
            assigneeRefs.stream().filter(ref -> users.containsKey(ref.userId())).toList(),
            TaskAssigneeRefDto::taskId,
            ref -> users.get(ref.userId()));

        return Lookups.map(tasks, task -> {
            BoardSummaryDto board = boards.get(task.getBoardId());
            return taskMapper.toListItemResponse(
                task,
                taskKey(boards, task),
                board != null ? board.name() : null,
                columnCategories.get(task.getColumnId()),
                today,
                assigneesByTask.getOrDefault(task.getId(), List.of()),
                subtaskCounts.get(task.getId()),
                // komentar dimiliki modul comment yang belum tersedia
                0);
        });
    }

    /** Filter {@code boardId} memerlukan akses ke board; tanpa filter → semua board tempat user punya task. */
    private List<UUID> resolveMyTaskBoardIds(UUID userId, UUID boardId) {
        if (boardId == null) {
            return taskRepository.findBoardIdsAssignedTo(userId);
        }
        if (!boardClient.hasAccess(boardId, userId)) {
            throw new ResourceNotFoundException(CommonErrorCode.BOARD_NOT_FOUND,
                Messages.get("task.error.board_not_found"));
        }
        return List.of(boardId);
    }

    private static Specification<Task> myTaskSpec(UUID userId, List<UUID> boardIds, MyTaskFilter filter,
        Map<UUID, String> columnCategories, LocalDate today) {
        Specification<Task> spec = TaskSpec.isActive()
            .and(TaskSpec.assignedTo(userId))
            .and(TaskSpec.inBoards(boardIds));

        if (filter.priority() != null) {
            spec = spec.and(TaskSpec.hasPriority(filter.priority()));
        }
        if (filter.status() != null) {
            Predicate<String> statusMatch = TaskStatusFilter.TODO.equals(filter.status())
                ? TODO_CATEGORIES::contains
                : filter.status()::equals;
            spec = spec.and(TaskSpec.inColumns(columnsWhere(columnCategories, statusMatch)));
        }
        if (!Boolean.TRUE.equals(filter.includeDone())) {
            spec = spec.and(TaskSpec.notInColumns(columnsWhere(columnCategories, TaskCategory.DONE::equals)));
        }
        if (filter.due() != null) {
            switch (filter.due()) {
                case TaskDueFilter.TODAY -> spec = spec.and(TaskSpec.dueOn(today));
                case TaskDueFilter.OVERDUE -> spec = spec.and(TaskSpec.dueBefore(today));
                case TaskDueFilter.UPCOMING -> spec = spec.and(
                    TaskSpec.dueBetween(today, today.plusDays(TaskLimits.UPCOMING_WINDOW_DAYS - 1L)));
                default -> {
                    // TaskDueFilter.ALL: tanpa batas tanggal
                }
            }
        }
        return spec;
    }

    private static Set<UUID> columnsWhere(Map<UUID, String> columnCategories, Predicate<String> categoryMatch) {
        Set<UUID> columnIds = new HashSet<>();
        columnCategories.forEach((columnId, category) -> {
            if (category != null && categoryMatch.test(category)) {
                columnIds.add(columnId);
            }
        });
        return columnIds;
    }

    private void syncAssignees(Task task, UUID actorId, List<UUID> requestedIds, List<TaskActivities> activities) {
        List<UUID> currentIds = taskAssigneeRepository.findAssigneeUserIds(task.getId());
        Set<UUID> targetIds = new LinkedHashSet<>(requestedIds);
        targetIds.remove(null);

        Set<UUID> removedIds = new LinkedHashSet<>(currentIds);
        removedIds.removeAll(targetIds);
        Set<UUID> addedIds = new LinkedHashSet<>(targetIds);
        addedIds.removeAll(currentIds);
        if (removedIds.isEmpty() && addedIds.isEmpty()) {
            return;
        }

        for (UUID userId : addedIds) {
            if (!boardClient.hasAccess(task.getBoardId(), userId)) {
                throw new BusinessException(CommonErrorCode.VALIDATION,
                    Messages.get("task.error.assignee_not_member"));
            }
        }
        if (!removedIds.isEmpty()) {
            taskAssigneeRepository.removeAssignees(task.getId(), removedIds);
        }
        if (!addedIds.isEmpty()) {
            taskAssigneeRepository.saveAll(Lookups.map(addedIds, userId -> newAssignee(task, userId)));
        }
        activities.add(newActivity(task, actorId, TaskActivityField.ASSIGNEES, currentIds, List.copyOf(targetIds)));
    }

    private void addFieldActivity(List<TaskActivities> activities, Task task, UUID actorId, String field,
        Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            activities.add(newActivity(task, actorId, field, oldValue, newValue));
        }
    }

    private TaskActivities newActivity(Task task, UUID actorId, String field, Object oldValue, Object newValue) {
        TaskActivities activity = new TaskActivities();
        activity.setTenantId(task.getTenantId());
        activity.setTaskId(task.getId());
        activity.setActorId(actorId);
        activity.setAction(TaskActivityAction.UPDATED);
        activity.setField(field);
        activity.setOldValue(toActivityJson(oldValue));
        activity.setNewValue(toActivityJson(newValue));
        return activity;
    }

    /** Nilai activity disimpan sebagai {@code {"value": ...}}; tanggal, UUID, dan daftar ditulis sebagai string. */
    private String toActivityJson(Object value) {
        if (value == null) {
            return null;
        }
        Object plain = value instanceof Collection<?> values
            ? values.stream().map(String::valueOf).toList()
            : value.toString();
        return taskMapper.mapToJson(Collections.singletonMap(ACTIVITY_VALUE_KEY, plain));
    }

    private static TaskAssignees newAssignee(Task task, UUID userId) {
        TaskAssigneesId id = new TaskAssigneesId();
        id.setTaskId(task.getId());
        id.setUserId(userId);
        TaskAssignees assignee = new TaskAssignees();
        assignee.setId(id);
        assignee.setTenantId(task.getTenantId());
        return assignee;
    }

    private static UUID otherTaskId(TaskDepedencies dependency, UUID taskId) {
        UUID blockingId = dependency.getId().getBlockingTaskId();
        return taskId.equals(blockingId) ? dependency.getId().getBlockedTaskId() : blockingId;
    }

    // ===================== Lookup lintas fitur/modul =====================

    /** BoardClient belum menyediakan batch lookup; dipanggil sekali per board unik, bukan per task. */
    private Map<UUID, BoardSummaryDto> loadBoards(Set<UUID> boardIds) {
        List<BoardSummaryDto> boards = new ArrayList<>();
        for (UUID boardId : boardIds) {
            boardClient.findById(boardId).ifPresent(boards::add);
        }
        return Lookups.toMap(boards, BoardSummaryDto::id);
    }

    private Map<UUID, TaskUserDto> loadUsers(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return Lookups.toMap(
            userClient.findSummariesByIds(userIds),
            UserSummaryDto::id,
            user -> new TaskUserDto(user.id(), user.name(), initials(user.name()), user.avatarUrl()));
    }

    private String taskKey(Map<UUID, BoardSummaryDto> boards, Task task) {
        BoardSummaryDto board = boards.get(task.getBoardId());
        if (board == null || task.getNumber() == null) {
            return null;
        }
        return taskMapper.toKey(board.key(), task.getNumber());
    }

    private LocalDate tenantToday(UUID tenantId) {
        String timezone = tenantClient.getTenantDetail(tenantId)
            .map(TenantDetailDto::timezone)
            .orElse(null);
        ZoneId zone = ZoneId.of(TaskDueDate.DEFAULT_TIMEZONE);
        if (timezone != null) {
            try {
                zone = ZoneId.of(timezone);
            } catch (DateTimeException e) {
                log.warn("Timezone tenant tidak valid, memakai default: tenantId={}, timezone={}", tenantId, timezone);
            }
        }
        return LocalDate.now(zone);
    }

    private static ResourceNotFoundException taskNotFound() {
        return new ResourceNotFoundException(CommonErrorCode.TASK_NOT_FOUND, Messages.get("task.error.not_found"));
    }

    private static int resolveLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return TaskLimits.DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, TaskLimits.MAX_PAGE_SIZE);
    }

    /** Cursor opaque = Base64url JSON kecil; cursor kosong/tidak valid diperlakukan sebagai halaman pertama. */
    private static Optional<Long> decodeCursorNumber(String cursor, Pattern field) {
        if (cursor == null || cursor.isBlank()) {
            return Optional.empty();
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
            Matcher matcher = field.matcher(json);
            return matcher.find() ? Optional.of(Long.parseLong(matcher.group(1))) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String encodeCursor(String template, long value) {
        String json = String.format(Locale.ROOT, template, value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(INITIALS_SINGLE_WORD_LENGTH, parts[0].length()))
                .toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
