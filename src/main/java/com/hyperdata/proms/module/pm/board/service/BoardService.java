package com.hyperdata.proms.module.pm.board.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.module.communication.client.contract.ChatClient;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.pm.board.constant.BoardCacheNames;
import com.hyperdata.proms.module.pm.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.pm.board.constant.BoardStatus;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardMemberCountDto;
import com.hyperdata.proms.module.pm.board.dto.BoardMemberRoleDto;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardColumn;
import com.hyperdata.proms.module.pm.board.entity.BoardMember;
import com.hyperdata.proms.module.pm.board.entity.Workstream;
import com.hyperdata.proms.module.pm.board.mapper.BoardMapper;
import com.hyperdata.proms.module.pm.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.pm.board.repository.BoardRepository;
import com.hyperdata.proms.module.pm.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.pm.board.repository.spec.BoardSpecs;
import com.hyperdata.proms.module.pm.client.contract.TaskClient;
import com.hyperdata.proms.module.pm.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.client.dto.CreateTaskCommand;
import com.hyperdata.proms.module.pm.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.client.dto.TaskSummaryDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final WorkstreamRepository workstreamRepository;
    private final UserClient userClient;
    private final TaskClient taskClient;
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
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Access denied to board", HttpStatus.FORBIDDEN);
        }

        List<BoardColumn> columns = boardColumnRepository.findActiveColumnsByBoardId(boardId);
        List<BoardDetailResponse.BoardColumnResponse> columnResponses = Lookups.map(columns,
            boardMapper::toColumnResponse);

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
            throw new BusinessException(CommonErrorCode.BOARD_ARCHIVED, "Cannot create task on archived board",
                HttpStatus.CONFLICT);
        }

        Optional<BoardMember> memberOpt = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.userId());
        boolean isOwner = board.getOwnerId().equals(currentUser.userId()) ||
            (memberOpt.isPresent() && Boolean.TRUE.equals(memberOpt.get().getIsOwner()));

        if (!isOwner && memberOpt.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Access denied to board", HttpStatus.FORBIDDEN);
        }

        if (memberOpt.isPresent() && BoardMemberRole.VIEWER.equalsIgnoreCase(memberOpt.get().getRole())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Viewers cannot create tasks", HttpStatus.FORBIDDEN);
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

    public List<ChatChannelSummaryDto> getBoardChatChannels(UUID boardId, CurrentUser currentUser) {
        checkBoardAccess(boardId, currentUser);
        return chatClient.listChannelsByBoardId(currentUser.activeTenantId(), boardId);
    }

    public void checkBoardAccess(UUID boardId, CurrentUser currentUser) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (!board.getTenantId().equals(currentUser.activeTenantId())) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        boolean isOwner = board.getOwnerId().equals(currentUser.userId());
        if (!isOwner && boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.userId()).isEmpty()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Access denied to board", HttpStatus.FORBIDDEN);
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
