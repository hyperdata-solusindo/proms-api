package com.hyperdata.proms.module.work.board.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.TenantScoped;
import com.hyperdata.proms.common.util.ConcurrencyGuard;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.communication.client.contract.ChatClient;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.work.board.constant.BoardCacheNames;
import com.hyperdata.proms.module.work.board.constant.BoardHealth;
import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.constant.BoardStatus;
import com.hyperdata.proms.module.work.board.constant.BoardInvitationResult;
import com.hyperdata.proms.module.work.board.dto.BatchInviteResultResponse;
import com.hyperdata.proms.module.work.board.dto.BoardCardResponse;
import com.hyperdata.proms.module.work.board.dto.BoardClientDto;
import com.hyperdata.proms.module.work.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.work.board.dto.BoardGroupDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberAvatarDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberCountDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberItemDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberListResponse;
import com.hyperdata.proms.module.work.board.dto.BoardMemberRoleDto;
import com.hyperdata.proms.module.work.board.dto.BoardPageMetaDto;
import com.hyperdata.proms.module.work.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.work.board.dto.BoardTaskCountsDto;
import com.hyperdata.proms.module.work.board.dto.BoardUserDto;
import com.hyperdata.proms.module.work.board.dto.CreateBoardRequest;
import com.hyperdata.proms.module.work.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.work.board.dto.InviteBoardMembersRequest;
import com.hyperdata.proms.module.work.board.dto.InviteItemResultDto;
import com.hyperdata.proms.module.work.board.dto.PendingInvitationDto;
import com.hyperdata.proms.module.work.board.dto.SavedViewResponse;
import com.hyperdata.proms.module.work.board.dto.CreateSavedViewRequest;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardMemberRequest;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.work.board.entity.Board;
import com.hyperdata.proms.module.work.board.entity.BoardColumn;
import com.hyperdata.proms.module.work.board.entity.BoardInvitation;
import com.hyperdata.proms.module.work.board.entity.BoardMember;
import com.hyperdata.proms.module.work.board.entity.BoardMemberWorkstream;
import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import com.hyperdata.proms.module.work.board.entity.TemplateColumn;
import com.hyperdata.proms.module.work.board.entity.WorkflowTemplate;
import com.hyperdata.proms.module.work.board.entity.Workstream;
import com.hyperdata.proms.module.work.board.mapper.BoardMapper;
import com.hyperdata.proms.module.work.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.work.board.repository.BoardInvitationRepository;
import com.hyperdata.proms.module.work.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.work.board.repository.BoardMemberWorkstreamRepository;
import com.hyperdata.proms.module.work.board.repository.BoardRepository;
import com.hyperdata.proms.module.work.board.repository.BoardSavedViewRepository;
import com.hyperdata.proms.module.work.board.repository.WorkflowTemplateRepository;
import com.hyperdata.proms.module.work.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.work.board.repository.spec.BoardSpecs;
import com.hyperdata.proms.module.work.client.contract.TaskClient;
import com.hyperdata.proms.module.work.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.work.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskMetricsDto;
import com.hyperdata.proms.module.work.client.dto.TaskSummaryDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@TenantScoped
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final WorkstreamRepository workstreamRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final BoardInvitationRepository boardInvitationRepository;
    private final BoardMemberWorkstreamRepository boardMemberWorkstreamRepository;
    private final BoardSavedViewRepository boardSavedViewRepository;
    private final UserClient userClient;
    private final TaskClient taskClient;
    private final TenantClient tenantClient;
    private final ChatClient chatClient;
    private final BoardMapper boardMapper;

    public PageResponse<BoardSummaryResponse> listBoards(
        CurrentUser currentUser,
        String tab,
        String q,
        Pageable pageable) {
        Specification<Board> spec = BoardSpecs.withFilters(
            currentUser.activeTenantId(),
            currentUser.userId(),
            tab,
            q);

        Page<Board> page = boardRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return PageResponse.from(page, List.of());
        }

        Set<UUID> pmIds = Lookups.extractIds(page.getContent(), Board::getPmId);
        List<UserSummaryDto> pmUsers = userClient.findSummariesByIds(pmIds);
        Map<UUID, UserSummaryDto> pmMap = Lookups.toMap(pmUsers, UserSummaryDto::id);

        Set<UUID> boardIds = Lookups.extractIds(page.getContent(), Board::getId);

        List<BoardMemberCountDto> memberCountDtos = boardMemberRepository.countMembersGroupedByBoardIds(boardIds);
        Map<UUID, Integer> memberCounts = Lookups.toMap(
            memberCountDtos,
            BoardMemberCountDto::boardId,
            dto -> (int) dto.count());

        List<BoardMemberRoleDto> roleDtos = boardMemberRepository.findRolesByUserAndBoardIds(currentUser.userId(),
            boardIds);
        Map<UUID, String> myRoles = Lookups.toMap(
            roleDtos,
            BoardMemberRoleDto::boardId,
            BoardMemberRoleDto::role);

        List<BoardSummaryResponse> items = page.getContent().stream().map(board -> {
            BoardSummaryResponse.PmSummaryResponse pmSummary = null;
            if (board.getPmId() != null && pmMap.containsKey(board.getPmId())) {
                pmSummary = new BoardSummaryResponse.PmSummaryResponse(
                    board.getPmId(),
                    pmMap.get(board.getPmId()).name());
            }

            int memberCount = memberCounts.getOrDefault(board.getId(), 0);
            String role = myRoles.getOrDefault(board.getId(),
                board.getOwnerId().equals(currentUser.userId()) ? BoardMemberRole.OWNER : null);

            return boardMapper.toSummaryResponse(board, pmSummary, memberCount, role);
        }).toList();

        return PageResponse.from(page, items);
    }

    @Cacheable(cacheNames = BoardCacheNames.BOARD_DETAIL, key = "#boardId", unless = "#result == null")
    public BoardDetailResponse getBoardDetail(UUID boardId, CurrentUser currentUser) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (!board.getTenantId().equals(currentUser.activeTenantId())) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        Optional<BoardMember> memberOpt = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.userId());
        boolean isOwner = board.getOwnerId().equals(currentUser.userId()) ||
            (memberOpt.isPresent() && Boolean.TRUE.equals(memberOpt.get().getIsOwner()));

        if (!isOwner && memberOpt.isEmpty()) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                Messages.get("board.error.access_denied"),
                HttpStatus.FORBIDDEN);
        }

        List<BoardColumn> columns = boardColumnRepository.findActiveColumnsByBoardId(boardId);
        Map<UUID, Long> columnTaskCounts = taskClient.getColumnTaskCounts(boardId);
        List<BoardDetailResponse.BoardColumnResponse> columnResponses = columns.stream()
            .map(c -> boardMapper.toColumnResponse(c, columnTaskCounts.getOrDefault(c.getId(), 0L).intValue()))
            .toList();

        List<Workstream> workstreams = workstreamRepository.findActiveWorkstreamsByBoardId(boardId);
        List<BoardDetailResponse.WorkstreamResponse> workstreamResponses = Lookups.map(workstreams,
            boardMapper::toWorkstreamResponse);

        List<BoardMember> members = boardMemberRepository.findByBoardId(boardId);
        Set<UUID> memberUserIds = Lookups.extractIds(members, BoardMember::getUserId);
        List<UserSummaryDto> memberUsers = userClient.findSummariesByIds(memberUserIds);
        Map<UUID, UserSummaryDto> userMap = Lookups.toMap(memberUsers, UserSummaryDto::id);

        List<BoardDetailResponse.BoardMemberResponse> memberResponses = members.stream().map(m -> {
            UserSummaryDto u = userMap.get(m.getUserId());
            String fullName = u != null ? u.name() : "Member";
            String avatarUrl = u != null ? u.avatarUrl() : null;
            String initials = getInitials(fullName);
            return new BoardDetailResponse.BoardMemberResponse(
                m.getUserId(),
                fullName,
                avatarUrl,
                initials,
                m.getRole(),
                m.getIsOwner());
        }).toList();

        String myRole = memberOpt.map(BoardMember::getRole)
            .orElse(isOwner ? BoardMemberRole.OWNER : BoardMemberRole.VIEWER);
        boolean canManageStructure = isOwner || BoardMemberRole.LEAD.equalsIgnoreCase(myRole)
            || BoardMemberRole.ADMIN.equalsIgnoreCase(myRole);
        boolean canCreateTask = !BoardStatus.ARCHIVED.equalsIgnoreCase(board.getStatus())
            && !BoardMemberRole.VIEWER.equalsIgnoreCase(myRole);
        boolean canArchiveBoard = isOwner;

        BoardDetailResponse.BoardPermissionResponse permissions = new BoardDetailResponse.BoardPermissionResponse(
            canManageStructure,
            canCreateTask,
            canArchiveBoard);

        return new BoardDetailResponse(
            board.getId(),
            board.getKey(),
            board.getName(),
            board.getStatus(),
            workstreamResponses,
            columnResponses,
            memberResponses,
            permissions,
            board.getUpdatedAt());
    }

    public PageResponse<TaskSummaryDto> getBoardTasks(
        UUID boardId,
        CurrentUser currentUser,
        BoardTaskFilterDto filter) {
        checkBoardAccess(boardId, currentUser);
        return taskClient.getTasksByBoard(boardId, filter);
    }

    @Transactional
    public TaskCreatedDto createBoardTask(
        UUID boardId,
        CurrentUser currentUser,
        CreateBoardTaskRequest request) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (!board.getTenantId().equals(currentUser.activeTenantId())) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        if (BoardStatus.ARCHIVED.equalsIgnoreCase(board.getStatus())) {
            throw new BusinessException(
                CommonErrorCode.BOARD_ARCHIVED,
                Messages.get("board.error.archived"),
                HttpStatus.CONFLICT);
        }

        Optional<BoardMember> memberOpt = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.userId());
        boolean isOwner = board.getOwnerId().equals(currentUser.userId()) ||
            (memberOpt.isPresent() && Boolean.TRUE.equals(memberOpt.get().getIsOwner()));

        if (!isOwner && memberOpt.isEmpty()) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                Messages.get("board.error.access_denied"),
                HttpStatus.FORBIDDEN);
        }

        if (memberOpt.isPresent() && BoardMemberRole.VIEWER.equalsIgnoreCase(memberOpt.get().getRole())) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                Messages.get("board.error.viewer_cannot_create"),
                HttpStatus.FORBIDDEN);
        }

        boardColumnRepository.findActiveColumnById(request.columnId(), boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", request.columnId()));

        if (request.workstreamId() != null) {
            workstreamRepository.findActiveWorkstreamById(request.workstreamId(), boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Workstream", request.workstreamId()));
        }

        CreateTaskCommand command = new CreateTaskCommand(
            board.getKey(),
            request.workstreamId(),
            request.columnId(),
            request.title(),
            request.assigneeIds(),
            request.dueDate(),
            request.priority(),
            currentUser.userId());

        return taskClient.createTask(boardId, command);
    }

    @Transactional
    @CacheEvict(cacheNames = BoardCacheNames.BOARD_DETAIL, key = "#boardId")
    public BoardDetailResponse updateBoard(
        UUID boardId,
        CurrentUser currentUser,
        UpdateBoardRequest request) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (!board.getTenantId().equals(currentUser.activeTenantId())) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        if (BoardStatus.ARCHIVED.equalsIgnoreCase(board.getStatus())) {
            throw new BusinessException(
                CommonErrorCode.BOARD_ARCHIVED,
                Messages.get("board.error.archived"),
                HttpStatus.CONFLICT);
        }

        Optional<BoardMember> memberOpt = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.userId());
        boolean isOwner = board.getOwnerId().equals(currentUser.userId()) ||
            (memberOpt.isPresent() && Boolean.TRUE.equals(memberOpt.get().getIsOwner()));

        if (!isOwner && memberOpt.isEmpty()) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                Messages.get("board.error.access_denied"),
                HttpStatus.FORBIDDEN);
        }

        if (memberOpt.isPresent() && BoardMemberRole.VIEWER.equalsIgnoreCase(memberOpt.get().getRole())) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                Messages.get("board.error.viewer_cannot_update"),
                HttpStatus.FORBIDDEN);
        }

        ConcurrencyGuard.requireMatch(board.getUpdatedAt(), () -> getBoardDetail(boardId, currentUser));

        board.setName(request.name());
        board.setDescription(request.description());
        if (request.clientName() != null) {
            board.setClientName(request.clientName());
        }
        if (request.groupName() != null) {
            board.setGroupName(request.groupName());
        }
        if (request.pmId() != null) {
            board.setPmId(request.pmId());
        }
        if (request.startDate() != null) {
            board.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            board.setDueDate(request.dueDate());
        }
        board.setUpdatedAt(Instant.now());

        boardRepository.save(board);
        return getBoardDetail(boardId, currentUser);
    }

    @Transactional
    public BoardDetailResponse createBoard(CurrentUser currentUser, CreateBoardRequest request) {
        CreateBoardRequest.VALIDATOR.validate(request)
            .throwIfInvalid(
                v -> new BusinessException(CommonErrorCode.VALIDATION, v.toString(), HttpStatus.UNPROCESSABLE_ENTITY));

        WorkflowTemplate template = null;
        if (request.templateId() != null) {
            template = workflowTemplateRepository.findByIdAndTenant(request.templateId(), currentUser.activeTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", request.templateId()));
        } else {
            template = workflowTemplateRepository.findGlobalDefaultTemplate().orElse(null);
        }

        Board board = new Board();
        board.setTenantId(currentUser.activeTenantId());
        board.setName(request.name());
        board.setKey(request.key());
        board.setClientName(request.clientName());
        board.setGroupName(request.groupName());
        board.setOwnerId(currentUser.userId());
        board.setCreatedBy(currentUser.userId());
        board.setPmId(request.pmId() != null ? request.pmId() : currentUser.userId());
        board.setStartDate(request.startDate());
        board.setDueDate(request.dueDate());
        board.setStatus(BoardStatus.ACTIVE);
        board.setHealth(BoardHealth.ON_TRACK);
        board.setHealthReason("Board baru dibuat");
        board.setProgressPct(0);
        board.setTaskSeq(0);
        board.setCreatedAt(Instant.now());
        board.setUpdatedAt(Instant.now());

        Board savedBoard = boardRepository.saveAndFlush(board);

        // Copy columns from template
        if (template != null && template.getColumns() != null && !template.getColumns().isEmpty()) {
            for (TemplateColumn tc : template.getColumns()) {
                BoardColumn col = new BoardColumn();
                col.setTenantId(currentUser.activeTenantId());
                col.setBoardId(savedBoard.getId());
                col.setName(tc.getName());
                col.setCategory(tc.getCategory());
                col.setWipLimit(tc.getWipLimit());
                col.setPosition(tc.getPosition());
                col.setCreatedAt(Instant.now());
                col.setUpdatedAt(Instant.now());
                boardColumnRepository.save(col);
            }
        }

        // Add creator as owner & admin member
        BoardMember member = new BoardMember();
        member.setTenantId(currentUser.activeTenantId());
        member.setBoardId(savedBoard.getId());
        member.setUserId(currentUser.userId());
        member.setRole(BoardMemberRole.ADMIN);
        member.setIsOwner(true);
        member.setCreatedAt(Instant.now());
        member.setUpdatedAt(Instant.now());
        boardMemberRepository.save(member);

        return getBoardDetail(savedBoard.getId(), currentUser);
    }

    @Transactional
    @CacheEvict(cacheNames = BoardCacheNames.BOARD_DETAIL, key = "#boardId")
    public void archiveBoard(UUID boardId, CurrentUser currentUser, String ifMatch) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (!board.getTenantId().equals(currentUser.activeTenantId())) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        ConcurrencyGuard.requireMatch(board.getUpdatedAt(), () -> getBoardDetail(boardId, currentUser));

        boolean isOwner = board.getOwnerId().equals(currentUser.userId());
        if (!isOwner) {
            Optional<BoardMember> memberOpt = boardMemberRepository.findByBoardIdAndUserId(boardId,
                currentUser.userId());
            if (memberOpt.isEmpty() || !Boolean.TRUE.equals(memberOpt.get().getIsOwner())) {
                throw new BusinessException(
                    CommonErrorCode.FORBIDDEN,
                    Messages.get("board.error.access_denied"),
                    HttpStatus.FORBIDDEN);
            }
        }

        board.setStatus(BoardStatus.ARCHIVED);
        board.setArchivedAt(Instant.now());
        board.setArchivedBy(currentUser.userId());
        board.setUpdatedAt(Instant.now());
        boardRepository.save(board);
    }

    public RestResponse<List<BoardCardResponse>> getBoardCards(
        CurrentUser currentUser,
        String tab,
        String groupId,
        String q,
        String cursor,
        int limit) {
        Specification<Board> spec = BoardSpecs.withFilters(
            currentUser.activeTenantId(),
            currentUser.userId(),
            tab,
            groupId,
            q);

        List<Board> boards = boardRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (boards.isEmpty()) {
            BoardPageMetaDto emptyMeta = new BoardPageMetaDto(
                0,
                null,
                new BoardPageMetaDto.BoardSummaryMetaDto(0, 0, 0),
                List.of(new BoardPageMetaDto.BoardGroupFilterChipDto("all", "All", 0)));
            return RestResponse.ok(List.of(), emptyMeta, "Berhasil");
        }

        // ZoneId for daysLeft computation
        ZoneId tenantZone = ZoneId.of("Asia/Jakarta");
        try {
            var tenantOpt = tenantClient.getTenantDetail(currentUser.activeTenantId());
            if (tenantOpt.isPresent() && tenantOpt.get().timezone() != null) {
                tenantZone = ZoneId.of(tenantOpt.get().timezone());
            }
        } catch (Exception ignored) {
            // fallback Asia/Jakarta
        }
        LocalDate todayInTenant = LocalDate.now(tenantZone);

        // Batch lookup metrics
        Set<UUID> boardIds = Lookups.extractIds(boards, Board::getId);
        Map<UUID, TaskMetricsDto> metricsMap = taskClient.getBoardTaskMetrics(boardIds);

        // Batch lookup members
        List<BoardMember> allMembers = boardMemberRepository.findMembersByBoardIds(boardIds);
        Map<UUID, List<BoardMember>> membersByBoard = new LinkedHashMap<>();
        for (BoardMember bm : allMembers) {
            membersByBoard.computeIfAbsent(bm.getBoardId(), k -> new ArrayList<>()).add(bm);
        }

        Set<UUID> topUserIds = allMembers.stream()
            .map(BoardMember::getUserId)
            .collect(java.util.stream.Collectors.toSet());
        List<UserSummaryDto> userSummaries = userClient.findSummariesByIds(topUserIds);
        Map<UUID, UserSummaryDto> userMap = Lookups.toMap(userSummaries, UserSummaryDto::id);

        // Group aggregation for chips
        Map<String, Long> groupCounts = new LinkedHashMap<>();
        long activeCount = 0;
        long onTrackCount = 0;
        long atRiskCount = 0;

        for (Board b : boards) {
            if (BoardStatus.ACTIVE.equalsIgnoreCase(b.getStatus()) && b.getArchivedAt() == null) {
                activeCount++;
            }
            if (BoardHealth.ON_TRACK.equalsIgnoreCase(b.getHealth())) {
                onTrackCount++;
            } else if (BoardHealth.AT_RISK.equalsIgnoreCase(b.getHealth())) {
                atRiskCount++;
            }
            String gName = b.getGroupName() != null && !b.getGroupName().isBlank() ? b.getGroupName() : "Other";
            groupCounts.put(gName, groupCounts.getOrDefault(gName, 0L) + 1);
        }

        List<BoardPageMetaDto.BoardGroupFilterChipDto> chipList = new ArrayList<>();
        chipList.add(new BoardPageMetaDto.BoardGroupFilterChipDto("all", "All", boards.size()));
        for (Map.Entry<String, Long> entry : groupCounts.entrySet()) {
            chipList
                .add(new BoardPageMetaDto.BoardGroupFilterChipDto(entry.getKey(), entry.getKey(), entry.getValue()));
        }

        BoardPageMetaDto meta = new BoardPageMetaDto(
            boards.size(),
            null,
            new BoardPageMetaDto.BoardSummaryMetaDto(activeCount, onTrackCount, atRiskCount),
            chipList);

        List<BoardCardResponse> cards = boards.stream().map(b -> {
            TaskMetricsDto metric = metricsMap.getOrDefault(b.getId(), TaskMetricsDto.empty());
            BoardTaskCountsDto taskCounts = new BoardTaskCountsDto(
                metric.todo(),
                metric.doing(),
                metric.blocked(),
                metric.done());

            Long daysLeft = null;
            if (b.getDueDate() != null) {
                daysLeft = ChronoUnit.DAYS.between(todayInTenant, b.getDueDate());
            }

            List<BoardMember> boardMems = membersByBoard.getOrDefault(b.getId(), List.of());
            int totalMems = boardMems.size();
            List<BoardMemberAvatarDto> topAvatars = boardMems.stream()
                .limit(3)
                .map(bm -> {
                    UserSummaryDto u = userMap.get(bm.getUserId());
                    String name = u != null ? u.name() : "Member";
                    String avatar = u != null ? u.avatarUrl() : null;
                    return new BoardMemberAvatarDto(bm.getUserId(), getInitials(name), avatar);
                })
                .toList();

            BoardGroupDto grp = b.getGroupName() != null
                ? new BoardGroupDto(b.getBoardGroupId(), b.getGroupName())
                : null;
            BoardClientDto cli = b.getClientName() != null ? new BoardClientDto(null, b.getClientName()) : null;

            return new BoardCardResponse(
                b.getId(),
                b.getKey(),
                b.getName(),
                grp,
                cli,
                b.getHealth() != null ? b.getHealth() : BoardHealth.ON_TRACK,
                b.getHealthReason(),
                metric.active(),
                taskCounts,
                metric.done(),
                metric.total(),
                metric.progressPct(),
                b.getDueDate(),
                daysLeft,
                topAvatars,
                totalMems,
                "\"" + (b.getUpdatedAt() != null ? b.getUpdatedAt().toEpochMilli() : 0) + "\"");
        }).toList();

        return RestResponse.ok(cards, meta, "Berhasil");
    }

    public BoardMemberListResponse getBoardMembers(CurrentUser currentUser, UUID boardId) {
        checkBoardAccess(boardId, currentUser);

        List<BoardMember> members = boardMemberRepository.findByBoardId(boardId);
        List<BoardInvitation> invitations = boardInvitationRepository.findPendingByBoardId(
            currentUser.activeTenantId(), boardId, Instant.now());

        Set<UUID> memberUserIds = Lookups.extractIds(members, BoardMember::getUserId);
        List<UserSummaryDto> memberUsers = userClient.findSummariesByIds(memberUserIds);
        Map<UUID, UserSummaryDto> userMap = Lookups.toMap(memberUsers, UserSummaryDto::id);

        Set<UUID> memberIds = Lookups.extractIds(members, BoardMember::getId);
        List<BoardMemberWorkstream> workstreams = boardMemberWorkstreamRepository.findByBoardMemberIdIn(memberIds);
        Map<UUID, List<UUID>> wsMap = new LinkedHashMap<>();
        for (BoardMemberWorkstream ws : workstreams) {
            wsMap.computeIfAbsent(ws.getBoardMemberId(), k -> new ArrayList<>()).add(ws.getWorkstreamId());
        }

        List<BoardMemberItemDto> memberDtos = members.stream().map(m -> {
            UserSummaryDto u = userMap.get(m.getUserId());
            BoardUserDto userDto = new BoardUserDto(
                m.getUserId(),
                u != null ? u.name() : "Member",
                u != null ? u.avatarUrl() : null,
                u != null ? u.email() : null);
            List<UUID> assignedWs = wsMap.getOrDefault(m.getId(), List.of());
            String version = "\"" + (m.getUpdatedAt() != null ? m.getUpdatedAt().toEpochMilli() : 0) + "\"";
            return new BoardMemberItemDto(
                m.getId(),
                userDto,
                m.getRole(),
                Boolean.TRUE.equals(m.getIsOwner()),
                assignedWs,
                version);
        }).toList();

        List<PendingInvitationDto> invitationDtos = invitations.stream()
            .map(inv -> new PendingInvitationDto(inv.getId(), inv.getEmail(), inv.getBoardRole(), inv.getExpiresAt()))
            .toList();

        return new BoardMemberListResponse(memberDtos, invitationDtos);
    }

    @Transactional
    public BatchInviteResultResponse inviteMembers(
        CurrentUser currentUser,
        UUID boardId,
        InviteBoardMembersRequest request) {
        checkBoardAccess(boardId, currentUser);

        InviteBoardMembersRequest.VALIDATOR.validate(request)
            .throwIfInvalid(
                v -> new BusinessException(CommonErrorCode.VALIDATION, v.toString(), HttpStatus.UNPROCESSABLE_ENTITY));

        List<String> emails = request.emails();
        Map<String, UserSummaryDto> existingUsers = userClient.findUsersByEmails(emails);

        List<BoardMember> currentMembers = boardMemberRepository.findByBoardId(boardId);
        Set<UUID> currentMemberUserIds = Lookups.extractIds(currentMembers, BoardMember::getUserId);

        List<InviteItemResultDto> results = new ArrayList<>();

        for (String email : emails) {
            String cleanEmail = email.trim().toLowerCase();
            UserSummaryDto existingUser = existingUsers.get(cleanEmail);

            if (existingUser != null) {
                if (currentMemberUserIds.contains(existingUser.id())) {
                    results.add(new InviteItemResultDto(cleanEmail, BoardInvitationResult.ALREADY_MEMBER, null, null));
                    continue;
                }

                // Add as member directly
                BoardMember newMember = new BoardMember();
                newMember.setTenantId(currentUser.activeTenantId());
                newMember.setBoardId(boardId);
                newMember.setUserId(existingUser.id());
                newMember.setRole(request.role());
                newMember.setIsOwner(false);
                newMember.setCreatedAt(Instant.now());
                newMember.setUpdatedAt(Instant.now());
                BoardMember savedMember = boardMemberRepository.save(newMember);

                if (request.workstreamIds() != null && !request.workstreamIds().isEmpty()) {
                    for (UUID wsId : request.workstreamIds()) {
                        boardMemberWorkstreamRepository.save(new BoardMemberWorkstream(
                            savedMember.getId(), wsId, currentUser.activeTenantId()));
                    }
                }

                currentMemberUserIds.add(existingUser.id());
                results
                    .add(new InviteItemResultDto(cleanEmail, BoardInvitationResult.ADDED, savedMember.getId(), null));
            } else {
                // Check if already invited and pending
                Optional<BoardInvitation> existingInv = boardInvitationRepository.findPendingByEmail(
                    currentUser.activeTenantId(), boardId, cleanEmail, Instant.now());
                if (existingInv.isPresent()) {
                    results.add(new InviteItemResultDto(cleanEmail, BoardInvitationResult.INVITED, null,
                        existingInv.get().getId()));
                    continue;
                }

                UUID[] wsArray = request.workstreamIds() != null
                    ? request.workstreamIds().toArray(new UUID[0])
                    : new UUID[0];
                BoardInvitation newInv = BoardInvitation.builder()
                    .tenantId(currentUser.activeTenantId())
                    .boardId(boardId)
                    .email(cleanEmail)
                    .boardRole(request.role())
                    .workstreamIds(wsArray)
                    .tokenHash(UUID.randomUUID().toString().replace("-", ""))
                    .invitedBy(currentUser.userId())
                    .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                    .build();
                BoardInvitation savedInv = boardInvitationRepository.save(newInv);

                results.add(new InviteItemResultDto(cleanEmail, BoardInvitationResult.INVITED, null, savedInv.getId()));
            }
        }

        return new BatchInviteResultResponse(results);
    }

    @Transactional
    public BoardMemberItemDto updateBoardMember(
        CurrentUser currentUser,
        UUID boardId,
        UUID memberId,
        UpdateBoardMemberRequest request,
        String ifMatch) {
        checkBoardAccess(boardId, currentUser);

        BoardMember member = boardMemberRepository
            .findByIdAndBoardIdAndTenantId(memberId, boardId, currentUser.activeTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("BoardMember", memberId));

        ConcurrencyGuard.requireMatch(member.getUpdatedAt(),
            () -> new BoardMemberItemDto(member.getId(), null, member.getRole(),
                Boolean.TRUE.equals(member.getIsOwner()), List.of(),
                "\"" + member.getUpdatedAt().toEpochMilli() + "\""));

        if (request.role() != null && !request.role().isBlank()) {
            member.setRole(request.role());
        }
        member.setUpdatedAt(Instant.now());
        boardMemberRepository.save(member);

        if (request.workstreamIds() != null) {
            boardMemberWorkstreamRepository.deleteByBoardMemberId(memberId);
            for (UUID wsId : request.workstreamIds()) {
                boardMemberWorkstreamRepository.save(new BoardMemberWorkstream(
                    memberId, wsId, currentUser.activeTenantId()));
            }
        }

        List<BoardMemberWorkstream> workstreams = boardMemberWorkstreamRepository.findByBoardMemberId(memberId);
        List<UUID> assignedWs = workstreams.stream().map(BoardMemberWorkstream::getWorkstreamId).toList();

        List<UserSummaryDto> userList = userClient.findSummariesByIds(List.of(member.getUserId()));
        UserSummaryDto u = !userList.isEmpty() ? userList.get(0) : null;
        BoardUserDto userDto = new BoardUserDto(
            member.getUserId(),
            u != null ? u.name() : "Member",
            u != null ? u.avatarUrl() : null,
            u != null ? u.email() : null);

        return new BoardMemberItemDto(
            member.getId(),
            userDto,
            member.getRole(),
            Boolean.TRUE.equals(member.getIsOwner()),
            assignedWs,
            "\"" + member.getUpdatedAt().toEpochMilli() + "\"");
    }

    @Transactional
    public void removeMember(CurrentUser currentUser, UUID boardId, UUID memberId, String ifMatch) {
        checkBoardAccess(boardId, currentUser);

        BoardMember member = boardMemberRepository
            .findByIdAndBoardIdAndTenantId(memberId, boardId, currentUser.activeTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("BoardMember", memberId));

        ConcurrencyGuard.requireMatch(member.getUpdatedAt(),
            () -> new BoardMemberItemDto(member.getId(), null, member.getRole(),
                Boolean.TRUE.equals(member.getIsOwner()), List.of(),
                "\"" + member.getUpdatedAt().toEpochMilli() + "\""));

        if (Boolean.TRUE.equals(member.getIsOwner())) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                "Pemilik board tidak dapat dihapus dari keanggotaan",
                HttpStatus.FORBIDDEN);
        }

        boardMemberWorkstreamRepository.deleteByBoardMemberId(memberId);
        boardMemberRepository.delete(member);

        // BR-DL-04: Unassign all tasks assigned to this user in this board
        taskClient.unassignUserFromBoardTasks(boardId, member.getUserId());
    }

    public List<ChatChannelSummaryDto> getBoardChatChannels(UUID boardId, CurrentUser currentUser) {
        checkBoardAccess(boardId, currentUser);
        return chatClient.listChannelsByBoardId(currentUser.activeTenantId(), boardId);
    }

    public List<SavedViewResponse> getSavedViews(CurrentUser user, UUID boardId) {
        checkBoardAccess(boardId, user);
        List<BoardSavedView> views = boardSavedViewRepository.findByTenantIdAndBoardIdAndUserId(
            user.activeTenantId(), boardId, user.userId());
        return views.stream().map(boardMapper::toSavedViewResponse).toList();
    }

    @Transactional
    public SavedViewResponse createSavedView(CurrentUser user, UUID boardId, CreateSavedViewRequest request) {
        CreateSavedViewRequest.VALIDATOR.validate(request)
            .throwIfInvalid(
                v -> new BusinessException(CommonErrorCode.VALIDATION, v.toString(), HttpStatus.UNPROCESSABLE_ENTITY));
        checkBoardAccess(boardId, user);

        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault) {
            boardSavedViewRepository.resetDefaultFlag(user.activeTenantId(), boardId, user.userId());
        }

        int nextPosition = boardSavedViewRepository.findMaxPosition(user.activeTenantId(), boardId, user.userId()) + 1;

        BoardSavedView view = BoardSavedView.builder()
            .tenantId(user.activeTenantId())
            .boardId(boardId)
            .userId(user.userId())
            .name(request.name())
            .viewType(request.type())
            .filters(request.filters() != null ? request.filters() : Map.of())
            .sortConfig(request.sortConfig() != null ? request.sortConfig() : Map.of())
            .groupBy(request.groupBy())
            .isDefault(isDefault)
            .position(nextPosition)
            .build();

        BoardSavedView saved = boardSavedViewRepository.saveAndFlush(view);
        return boardMapper.toSavedViewResponse(saved);
    }

    public void checkBoardAccess(UUID boardId, CurrentUser currentUser) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (!board.getTenantId().equals(currentUser.activeTenantId())) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        boolean isOwner = board.getOwnerId().equals(currentUser.userId());
        if (!isOwner && boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.userId()).isEmpty()) {
            throw new BusinessException(
                CommonErrorCode.FORBIDDEN,
                Messages.get("board.error.access_denied"),
                HttpStatus.FORBIDDEN);
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "??";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}
