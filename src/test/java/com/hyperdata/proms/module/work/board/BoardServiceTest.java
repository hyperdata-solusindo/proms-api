package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.IfMatchRequiredException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.exception.StaleVersionException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.communication.client.contract.ChatClient;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.work.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.work.board.dto.BoardMemberCountDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberRoleDto;
import com.hyperdata.proms.module.work.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.work.board.dto.CreateBoardRequest;
import com.hyperdata.proms.module.work.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.work.board.dto.CreateSavedViewRequest;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.work.board.entity.Board;
import com.hyperdata.proms.module.work.board.entity.BoardColumn;
import com.hyperdata.proms.module.work.board.entity.BoardMember;
import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import com.hyperdata.proms.module.work.board.entity.TemplateColumn;
import com.hyperdata.proms.module.work.board.entity.WorkflowTemplate;
import com.hyperdata.proms.module.work.board.entity.Workstream;
import com.hyperdata.proms.module.work.board.mapper.BoardMapper;
import com.hyperdata.proms.module.work.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.work.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.work.board.repository.BoardInvitationRepository;
import com.hyperdata.proms.module.work.board.repository.BoardMemberWorkstreamRepository;
import com.hyperdata.proms.module.work.board.repository.BoardRepository;
import com.hyperdata.proms.module.work.board.repository.BoardSavedViewRepository;
import com.hyperdata.proms.module.work.board.repository.WorkflowTemplateRepository;
import com.hyperdata.proms.module.work.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.work.board.service.BoardService;
import com.hyperdata.proms.module.work.client.contract.TaskClient;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskMetricsDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private UserClient userClient;

    @Mock
    private TaskClient taskClient;

    @Mock
    private TenantClient tenantClient;

    @Mock
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Mock
    private BoardInvitationRepository boardInvitationRepository;

    @Mock
    private BoardMemberWorkstreamRepository boardMemberWorkstreamRepository;

    @Mock
    private BoardSavedViewRepository boardSavedViewRepository;

    @Mock
    private ChatClient chatClient;

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

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
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

        given(userClient.findSummariesByIds(Set.of(pmId)))
            .willReturn(List.of(new UserSummaryDto(pmId, "Fariz PM", "fariz@proms.co.id", null)));

        given(boardMemberRepository.countMembersGroupedByBoardIds(Set.of(boardId)))
            .willReturn(List.of(new BoardMemberCountDto(boardId, 5L)));

        given(boardMemberRepository.findRolesByUserAndBoardIds(userId, Set.of(boardId)))
            .willReturn(List.of(new BoardMemberRoleDto(boardId, "owner")));

        given(taskClient.getBoardTaskMetrics(Set.of(boardId)))
            .willReturn(Map.of(boardId, new TaskMetricsDto(5, 2, 0, 1, 3, 2, 40)));

        PageResponse<BoardSummaryResponse> result = boardService.listBoards(currentUser, "mine", null, pageable);

        assertThat(result.items()).hasSize(1);
        BoardSummaryResponse item = result.items().get(0);
        assertThat(item.key()).isEqualTo("PRJ");
        assertThat(item.pm()).isNotNull();
        assertThat(item.pm().fullName()).isEqualTo("Fariz PM");
        assertThat(item.memberCount()).isEqualTo(5);
        assertThat(item.myRole()).isEqualTo("owner");
        assertThat(item.progressPct()).isEqualTo(40);
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
        given(userClient.findSummariesByIds(Set.of(userId)))
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
        given(taskClient.createTask(eq(boardId), any())).willReturn(createdDto);

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
        given(chatClient.listChannelsByBoardId(tenantId, boardId)).willReturn(List.of(channel));

        List<ChatChannelSummaryDto> channels = boardService.getBoardChatChannels(boardId, currentUser);

        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).name()).isEqualTo("general");
    }

    @Test
    @DisplayName("GIVEN matching If-Match WHEN updateBoard THEN updates board and returns detail")
    void givenMatchingIfMatch_whenUpdateBoard_thenSucceeds() {
        UUID boardId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-11-10T10:15:30.123456Z");

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId);
        board.setName("Original Board");
        board.setDescription("Old Description");
        board.setStatus("active");
        board.setUpdatedAt(updatedAt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.IF_MATCH, "\"" + updatedAt.toString() + "\"");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));
        given(boardRepository.save(any(Board.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateBoardRequest updateRequest = new UpdateBoardRequest("Updated Board", "New Description");
        BoardDetailResponse response = boardService.updateBoard(boardId, currentUser, updateRequest);

        assertThat(response.name()).isEqualTo("Updated Board");
        assertThat(board.getName()).isEqualTo("Updated Board");
        assertThat(board.getDescription()).isEqualTo("New Description");
    }

    @Test
    @DisplayName("GIVEN missing If-Match header WHEN updateBoard THEN throws IfMatchRequiredException")
    void givenMissingIfMatch_whenUpdateBoard_thenThrowsIfMatchRequiredException() {
        UUID boardId = UUID.randomUUID();
        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId);
        board.setStatus("active");
        board.setUpdatedAt(Instant.now());

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        UpdateBoardRequest updateRequest = new UpdateBoardRequest("Updated Board", "New Description");

        assertThatThrownBy(() -> boardService.updateBoard(boardId, currentUser, updateRequest))
            .isInstanceOf(IfMatchRequiredException.class);
    }

    @Test
    @DisplayName("GIVEN stale If-Match header WHEN updateBoard THEN throws StaleVersionException with current payload")
    void givenStaleIfMatch_whenUpdateBoard_thenThrowsStaleVersionException() {
        UUID boardId = UUID.randomUUID();
        Instant dbUpdatedAt = Instant.parse("2026-11-10T12:00:00Z");

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId);
        board.setName("Server State Name");
        board.setStatus("active");
        board.setUpdatedAt(dbUpdatedAt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.IF_MATCH, "\"2026-11-10T08:00:00Z\"");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        UpdateBoardRequest updateRequest = new UpdateBoardRequest("My Overwrite Attempt", "New Description");

        assertThatThrownBy(() -> boardService.updateBoard(boardId, currentUser, updateRequest))
            .isInstanceOf(StaleVersionException.class)
            .satisfies(ex -> {
                StaleVersionException sve = (StaleVersionException) ex;
                assertThat(sve.getCurrent()).isNotNull();
                assertThat(sve.getCurrent()).isInstanceOf(BoardDetailResponse.class);
                BoardDetailResponse currentDetail = (BoardDetailResponse) sve.getCurrent();
                assertThat(currentDetail.name()).isEqualTo("Server State Name");
            });
    }

    @Test
    @DisplayName("GIVEN create board request WHEN created THEN board and columns from template are persisted")
    void shouldCreateBoardWithTemplateColumnsAndAssignCreator() {
        var req = new CreateBoardRequest(
            "Proyek Alpha", "ALP", "Client X", "Dev", userId,
            LocalDate.now(), LocalDate.now().plusDays(20), null, null);

        var template = new WorkflowTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Default Scrum");

        var col = new TemplateColumn();
        col.setId(UUID.randomUUID());
        col.setTemplate(template);
        col.setName("To do");
        col.setCategory("todo");
        col.setPosition("0|hzzz:");
        template.getColumns().add(col);

        given(workflowTemplateRepository.findGlobalDefaultTemplate()).willReturn(Optional.of(template));
        given(boardRepository.saveAndFlush(any(Board.class))).willAnswer(inv -> {
            Board b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        given(boardRepository.findActiveById(any())).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            Board b = new Board();
            b.setId(id);
            b.setTenantId(tenantId);
            b.setName("Proyek Alpha");
            b.setKey("ALP");
            b.setOwnerId(userId);
            b.setStatus("active");
            b.setUpdatedAt(Instant.now());
            return Optional.of(b);
        });

        BoardDetailResponse response = boardService.createBoard(currentUser, req);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Proyek Alpha");
    }

    @Test
    @DisplayName("GIVEN getBoardCards WHEN called THEN enriches task metrics, daysLeft and members")
    void shouldEnrichBoardCardsWithMetricsAndAvatars() {
        UUID boardId = UUID.randomUUID();
        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setName("Proyek Alpha");
        board.setKey("ALP");
        board.setOwnerId(userId);
        board.setStatus("active");
        board.setGroupName("Dev");
        board.setClientName("Client X");
        board.setDueDate(LocalDate.now().plusDays(10));
        board.setHealth("on_track");
        board.setUpdatedAt(Instant.now());

        given(boardRepository.findAll(any(Specification.class), any(Sort.class)))
            .willReturn(List.of(board));
        given(tenantClient.getTenantDetail(tenantId))
            .willReturn(Optional.of(new TenantDetailDto(tenantId, "test",
                "Tenant Test", "active", "Asia/Jakarta")));
        given(taskClient.getBoardTaskMetrics(any()))
            .willReturn(Map.of(boardId,
                new TaskMetricsDto(3, 0, 0, 0, 3, 3, 0)));

        var result = boardService.getBoardCards(currentUser, "mine", null, null, null, 20);
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).taskCounts().todo()).isEqualTo(3);
        assertThat(result.data().get(0).daysLeft()).isNotNull();
        assertThat(result.meta()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN member to remove WHEN removeMember called THEN deletes member and unassigns board tasks")
    void removeMemberShouldDeleteAndUnassignTasks() {
        UUID boardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId); // current user is owner

        BoardMember member = new BoardMember();
        member.setId(memberId);
        member.setBoardId(boardId);
        member.setTenantId(tenantId);
        member.setUserId(targetUserId);
        member.setRole("member");
        member.setIsOwner(false);
        Instant updatedAt = Instant.parse("2026-11-19T10:00:00Z");
        member.setUpdatedAt(updatedAt);

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));
        given(boardMemberRepository.findByIdAndBoardIdAndTenantId(memberId, boardId, tenantId))
            .willReturn(Optional.of(member));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.IF_MATCH, "\"" + updatedAt.toEpochMilli() + "\"");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        boardService.removeMember(currentUser, boardId, memberId, "\"" + updatedAt.toEpochMilli() + "\"");

        org.mockito.Mockito.verify(boardMemberRepository).delete(member);
        org.mockito.Mockito.verify(taskClient).unassignUserFromBoardTasks(boardId, targetUserId);
    }

    @Test
    @DisplayName("createSavedView harus mereset default flag lama jika view baru ditandai isDefault")
    void createSavedViewShouldResetOldDefaultFlag() {
        UUID boardId = UUID.randomUUID();
        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId);

        var req = new CreateSavedViewRequest(
            "Default Kanban", "board", Map.of(), Map.of(), null, true);

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));
        given(boardSavedViewRepository.findMaxPosition(tenantId, boardId, userId)).willReturn(2);
        given(boardSavedViewRepository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        var response = boardService.createSavedView(currentUser, boardId, req);

        org.mockito.Mockito.verify(boardSavedViewRepository).resetDefaultFlag(tenantId, boardId, userId);
        assertThat(response.isDefault()).isTrue();
        assertThat(response.position()).isEqualTo(3);
        assertThat(response.name()).isEqualTo("Default Kanban");
    }

    @Test
    @DisplayName("getSavedViews harus mengembalikan daftar saved views milik user atau default")
    void getSavedViewsShouldReturnList() {
        UUID boardId = UUID.randomUUID();
        Board board = new Board();
        board.setId(boardId);
        board.setTenantId(tenantId);
        board.setOwnerId(userId);

        BoardSavedView view = BoardSavedView.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .boardId(boardId)
            .userId(userId)
            .name("My View")
            .viewType("list")
            .isDefault(false)
            .position(1)
            .updatedAt(Instant.now())
            .build();

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));
        given(boardSavedViewRepository.findByTenantIdAndBoardIdAndUserId(tenantId, boardId, userId))
            .willReturn(List.of(view));

        var result = boardService.getSavedViews(currentUser, boardId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("My View");
    }
}
