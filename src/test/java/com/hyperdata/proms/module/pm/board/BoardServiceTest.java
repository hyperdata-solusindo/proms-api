package com.hyperdata.proms.module.pm.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.communication.chat.facade.ChatFacade;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.identity.user.facade.dto.UserSummaryDto;
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
import com.hyperdata.proms.module.pm.board.service.BoardService;
import com.hyperdata.proms.module.pm.task.facade.TaskFacade;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private WorkstreamRepository workstreamRepository;

    @Mock
    private UserFacade userFacade;

    @Mock
    private TaskFacade taskFacade;

    @Mock
    private ChatFacade chatFacade;

    @Spy
    private BoardMapper boardMapper = Mappers.getMapper(BoardMapper.class);

    @InjectMocks
    private BoardService boardService;

    private UUID tenantId;
    private UUID userId;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "test@proms.co.id", tenantId, "member", 1L);
    }

    @Test
    @DisplayName("GIVEN list boards WHEN filtered by tab THEN returns enriched PageResponse")
    void givenListBoards_whenFilteredByTab_thenReturnsEnrichedPageResponse() {
        UUID boardId = UUID.randomUUID();
        UUID pmId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setKey("PRJ");
        board.setName("Project Core");
        board.setStatus("active");
        board.setOwnerId(userId);
        board.setPmId(pmId);
        board.setProgressPct(60);
        board.setDueDate(LocalDate.now().plusDays(10));
        board.setUpdatedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 10);
        given(boardRepository.findAll(any(Specification.class), eq(pageable)))
            .willReturn(new PageImpl<>(List.of(board), pageable, 1));

        given(userFacade.findSummariesByIds(Set.of(pmId)))
            .willReturn(List.of(new UserSummaryDto(pmId, "Fariz PM", "fariz@proms.co.id", null)));

        given(boardMemberRepository.countMembersGroupedByBoardIds(Set.of(boardId)))
            .willReturn(List.of(new BoardMemberCountDto(boardId, 5L)));

        given(boardMemberRepository.findRolesByUserAndBoardIds(userId, Set.of(boardId)))
            .willReturn(List.of(new BoardMemberRoleDto(boardId, "owner")));

        PageResponse<BoardSummaryResponse> result = boardService.listBoards(currentUser, "mine", null, pageable);

        assertThat(result.items()).hasSize(1);
        BoardSummaryResponse item = result.items().get(0);
        assertThat(item.key()).isEqualTo("PRJ");
        assertThat(item.pm()).isNotNull();
        assertThat(item.pm().fullName()).isEqualTo("Fariz PM");
        assertThat(item.memberCount()).isEqualTo(5);
        assertThat(item.myRole()).isEqualTo("owner");
    }

    @Test
    @DisplayName("GIVEN existing board and member WHEN getBoardDetail THEN returns full detail with columns, workstreams, and permissions")
    void givenExistingBoardAndMember_whenGetBoardDetail_thenReturnsFullDetail() {
        UUID boardId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setKey("PRJ");
        board.setName("Project Core");
        board.setStatus("active");
        board.setOwnerId(userId);
        board.setUpdatedAt(Instant.now());

        BoardColumn col1 = new BoardColumn();
        col1.setId(UUID.randomUUID());
        col1.setBoardId(boardId);
        col1.setName("To Do");
        col1.setCategory("to_do");
        col1.setPosition("0|hzzzzz:");

        Workstream ws1 = new Workstream();
        ws1.setId(UUID.randomUUID());
        ws1.setBoardId(boardId);
        ws1.setName("Backend");
        ws1.setColor("#3B82F6");
        ws1.setPosition(1);

        BoardMember member = new BoardMember();
        member.setId(UUID.randomUUID());
        member.setBoardId(boardId);
        member.setUserId(userId);
        member.setRole("lead");
        member.setIsOwner(true);

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));
        given(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).willReturn(Optional.of(member));
        given(boardColumnRepository.findActiveColumnsByBoardId(boardId))
            .willReturn(List.of(col1));
        given(workstreamRepository.findActiveWorkstreamsByBoardId(boardId)).willReturn(List.of(ws1));
        given(boardMemberRepository.findByBoardId(boardId)).willReturn(List.of(member));
        given(userFacade.findSummariesByIds(Set.of(userId)))
            .willReturn(List.of(new UserSummaryDto(userId, "Alice Lead", "alice@proms.co.id", null)));

        BoardDetailResponse detail = boardService.getBoardDetail(boardId, currentUser);

        assertThat(detail.id()).isEqualTo(boardId);
        assertThat(detail.key()).isEqualTo("PRJ");
        assertThat(detail.columns()).hasSize(1);
        assertThat(detail.columns().get(0).name()).isEqualTo("To Do");
        assertThat(detail.workstreams()).hasSize(1);
        assertThat(detail.workstreams().get(0).name()).isEqualTo("Backend");
        assertThat(detail.members()).hasSize(1);
        assertThat(detail.members().get(0).fullName()).isEqualTo("Alice Lead");
        assertThat(detail.members().get(0).initials()).isEqualTo("AL");
        assertThat(detail.permissions().canCreateTask()).isTrue();
        assertThat(detail.permissions().canManageStructure()).isTrue();
    }

    @Test
    @DisplayName("GIVEN non-existing board WHEN getBoardDetail THEN throws ResourceNotFoundException")
    void givenNonExistingBoard_whenGetBoardDetail_thenThrowsResourceNotFoundException() {
        UUID boardId = UUID.randomUUID();
        given(boardRepository.findActiveById(boardId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardDetail(boardId, currentUser))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN valid request WHEN createBoardTask THEN validates hierarchy and delegates to TaskFacade")
    void givenValidRequest_whenCreateBoardTask_thenValidatesAndDelegatesToTaskFacade() {
        UUID boardId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        UUID workstreamId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setKey("PRJ");
        board.setStatus("active");
        board.setOwnerId(userId);

        BoardMember member = new BoardMember();
        member.setBoardId(boardId);
        member.setUserId(userId);
        member.setRole("member");

        BoardColumn col = new BoardColumn();
        col.setId(columnId);
        col.setBoardId(boardId);

        Workstream ws = new Workstream();
        ws.setId(workstreamId);
        ws.setBoardId(boardId);

        CreateBoardTaskRequest request = new CreateBoardTaskRequest(
            "Implement JWT",
            workstreamId,
            columnId,
            List.of(userId),
            LocalDate.now().plusDays(5),
            "high");

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));
        given(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).willReturn(Optional.of(member));
        given(boardColumnRepository.findActiveColumnById(columnId, boardId))
            .willReturn(Optional.of(col));
        given(workstreamRepository.findActiveWorkstreamById(workstreamId, boardId))
            .willReturn(Optional.of(ws));

        TaskCreatedDto createdDto = new TaskCreatedDto(
            UUID.randomUUID(),
            "PRJ-101",
            "Implement JWT",
            columnId,
            workstreamId,
            Instant.now());
        given(taskFacade.createTask(eq(boardId), any())).willReturn(createdDto);

        TaskCreatedDto result = boardService.createBoardTask(boardId, currentUser, request);

        assertThat(result.key()).isEqualTo("PRJ-101");
        assertThat(result.title()).isEqualTo("Implement JWT");
    }

    @Test
    @DisplayName("GIVEN archived board WHEN createBoardTask THEN throws BusinessException")
    void givenArchivedBoard_whenCreateBoardTask_thenThrowsBusinessException() {
        UUID boardId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setKey("PRJ");
        board.setStatus("archived");
        board.setOwnerId(userId);

        CreateBoardTaskRequest request = new CreateBoardTaskRequest(
            "Task on archived",
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null);

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        assertThatThrownBy(() -> boardService.createBoardTask(boardId, currentUser, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("archived");
    }

    @Test
    @DisplayName("GIVEN boardId WHEN getBoardChatChannels THEN delegates to ChatFacade")
    void givenBoardId_whenGetBoardChatChannels_thenDelegatesToChatFacade() {
        UUID boardId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId);

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        ChatChannelSummaryDto channel = new ChatChannelSummaryDto(
            UUID.randomUUID(),
            "board",
            boardId,
            "general",
            0,
            null,
            false,
            false);
        given(chatFacade.listChannelsByBoardId(tenantId, boardId)).willReturn(List.of(channel));

        List<ChatChannelSummaryDto> channels = boardService.getBoardChatChannels(boardId, currentUser);

        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).name()).isEqualTo("general");
    }
}
