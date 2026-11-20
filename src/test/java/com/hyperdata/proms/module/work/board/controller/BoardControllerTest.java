package com.hyperdata.proms.module.work.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyperdata.proms.common.exception.GlobalExceptionHandler;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.work.board.dto.BatchInviteResultResponse;
import com.hyperdata.proms.module.work.board.dto.BoardCardResponse;
import com.hyperdata.proms.module.work.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.work.board.dto.BoardMemberItemDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberListResponse;
import com.hyperdata.proms.module.work.board.dto.BoardPageMetaDto;
import com.hyperdata.proms.module.work.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.work.board.dto.BoardTaskCountsDto;
import com.hyperdata.proms.module.work.board.dto.CreateBoardRequest;
import com.hyperdata.proms.module.work.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.work.board.dto.CreateSavedViewRequest;
import com.hyperdata.proms.module.work.board.dto.InviteBoardMembersRequest;
import com.hyperdata.proms.module.work.board.dto.SavedViewResponse;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardMemberRequest;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.work.board.dto.WorkflowTemplateResponse;
import com.hyperdata.proms.module.work.board.service.BoardService;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskSummaryDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    @InjectMocks
    private BoardController boardController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CurrentUser currentUser;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(boardController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "test@proms.co.id", tenantId, "member", 1L);

        var auth = new UsernamePasswordAuthenticationToken(
            currentUser,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN authenticated user WHEN GET /api/v1/boards THEN returns 200 with list envelope")
    void givenAuthenticatedUser_whenListBoards_thenReturns200Envelope() throws Exception {
        UUID boardId = UUID.randomUUID();
        BoardSummaryResponse summary = new BoardSummaryResponse(
            boardId,
            "PRJ",
            "Project Alpha",
            null,
            "active",
            "on_track",
            null,
            45,
            LocalDate.now().plusDays(20),
            new BoardSummaryResponse.PmSummaryResponse(userId, "Fariz PM"),
            3,
            "owner",
            null,
            Instant.now());
        PageResponse<BoardSummaryResponse> pageResponse = new PageResponse<>(
            List.of(summary),
            0,
            20,
            1,
            1,
            true,
            true);

        given(boardService.listBoards(eq(currentUser), eq("mine"), any(), any()))
            .willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/boards").param("tab", "mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.items[0].key").value("PRJ"))
            .andExpect(jsonPath("$.data.items[0].name").value("Project Alpha"));
    }

    @Test
    @DisplayName("GIVEN valid boardId WHEN GET /api/v1/boards/{boardId} THEN returns 200 with detail envelope")
    void givenValidBoardId_whenGetBoardDetail_thenReturns200Envelope() throws Exception {
        UUID boardId = UUID.randomUUID();
        BoardDetailResponse detail = new BoardDetailResponse(
            boardId,
            "PRJ",
            "Project Alpha",
            "active",
            List.of(),
            List.of(),
            List.of(),
            new BoardDetailResponse.BoardPermissionResponse(true, true, true),
            Instant.now());

        given(boardService.getBoardDetail(eq(boardId), eq(currentUser)))
            .willReturn(detail);

        mockMvc.perform(get("/api/v1/boards/{boardId}", boardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.key").value("PRJ"))
            .andExpect(jsonPath("$.data.permissions.canCreateTask").value(true));
    }

    @Test
    @DisplayName("GIVEN valid boardId WHEN GET /api/v1/boards/{boardId}/tasks THEN returns 200 with tasks envelope")
    void givenValidBoardId_whenGetBoardTasks_thenReturns200Envelope() throws Exception {
        UUID boardId = UUID.randomUUID();
        PageResponse<TaskSummaryDto> tasks = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);

        given(boardService.getBoardTasks(eq(boardId), eq(currentUser), any()))
            .willReturn(tasks);

        mockMvc.perform(get("/api/v1/boards/{boardId}/tasks", boardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("GIVEN valid payload WHEN POST /api/v1/boards/{boardId}/tasks THEN returns 201 created envelope")
    void givenValidPayload_whenCreateBoardTask_thenReturns201Envelope() throws Exception {
        UUID boardId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        UUID workstreamId = UUID.randomUUID();

        CreateBoardTaskRequest request = new CreateBoardTaskRequest(
            "New Feature Task",
            workstreamId,
            columnId,
            List.of(userId),
            LocalDate.now().plusDays(7),
            "medium");

        TaskCreatedDto createdDto = new TaskCreatedDto(
            UUID.randomUUID(),
            "PRJ-102",
            "New Feature Task",
            columnId,
            workstreamId,
            Instant.now());

        given(boardService.createBoardTask(eq(boardId), eq(currentUser), eq(request)))
            .willReturn(createdDto);

        mockMvc.perform(post("/api/v1/boards/{boardId}/tasks", boardId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.key").value("PRJ-102"))
            .andExpect(jsonPath("$.data.title").value("New Feature Task"));
    }

    @Test
    @DisplayName("GIVEN invalid payload WHEN POST /api/v1/boards/{boardId}/tasks THEN returns 422 Validation error")
    void givenInvalidPayload_whenCreateBoardTask_thenReturns422Validation() throws Exception {
        UUID boardId = UUID.randomUUID();

        // Title is blank and workstreamId is null -> fails validation
        CreateBoardTaskRequest request = new CreateBoardTaskRequest(
            "",
            null,
            UUID.randomUUID(),
            null,
            null,
            "invalid_priority");

        mockMvc.perform(post("/api/v1/boards/{boardId}/tasks", boardId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.statusCode").value(422))
            .andExpect(jsonPath("$.error.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("GIVEN valid boardId WHEN GET /api/v1/boards/{boardId}/chat/channels THEN returns 200 with chat channels")
    void givenValidBoardId_whenGetBoardChatChannels_thenReturns200Envelope() throws Exception {
        UUID boardId = UUID.randomUUID();
        ChatChannelSummaryDto channel = new ChatChannelSummaryDto(
            UUID.randomUUID(),
            "board",
            boardId,
            "general",
            0,
            null,
            false,
            false);

        given(boardService.getBoardChatChannels(eq(boardId), eq(currentUser)))
            .willReturn(List.of(channel));

        mockMvc.perform(get("/api/v1/boards/{boardId}/chat/channels", boardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data[0].name").value("general"));
    }

    @Test
    @DisplayName("GIVEN unauthenticated request WHEN GET /api/v1/boards THEN returns 401 UNAUTHENTICATED")
    void givenUnauthenticatedRequest_whenListBoards_thenReturns401Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/boards"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.statusCode").value(401))
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.message").value("Autentikasi diperlukan untuk mengakses endpoint ini"));
    }

    @Test
    @DisplayName("GIVEN valid payload and header WHEN PUT /api/v1/boards/{boardId} THEN returns 200 updated envelope")
    void givenValidPayload_whenUpdateBoard_thenReturns200Envelope() throws Exception {
        UUID boardId = UUID.randomUUID();
        UpdateBoardRequest request = new UpdateBoardRequest("Updated Board Name", "New description");
        BoardDetailResponse updatedDetail = new BoardDetailResponse(
            boardId,
            "PRJ",
            "Updated Board Name",
            "active",
            List.of(),
            List.of(),
            List.of(),
            new BoardDetailResponse.BoardPermissionResponse(true, true, true),
            Instant.now());

        given(boardService.updateBoard(eq(boardId), eq(currentUser), any(UpdateBoardRequest.class)))
            .willReturn(updatedDetail);

        mockMvc.perform(put("/api/v1/boards/{boardId}", boardId)
            .header(HttpHeaders.IF_MATCH, "\"2026-11-10T10:00:00Z\"")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.name").value("Updated Board Name"))
            .andExpect(jsonPath("$.message").value("Board berhasil diperbarui"));
    }

    @Test
    @DisplayName("GIVEN valid request WHEN POST /api/v1/boards THEN returns 201 Created")
    void createBoardShouldReturn201() throws Exception {
        var req = new CreateBoardRequest(
            "Nusa Bioscience", "NB", "PT Nusa", "Dev", UUID.randomUUID(),
            LocalDate.now(), LocalDate.now().plusDays(30), null, null);

        UUID boardId = UUID.randomUUID();
        BoardDetailResponse detail = new BoardDetailResponse(
            boardId, "NB", "Nusa Bioscience", "active",
            List.of(), List.of(), List.of(),
            new BoardDetailResponse.BoardPermissionResponse(true, true, true),
            Instant.now());

        given(boardService.createBoard(any(), any())).willReturn(detail);

        mockMvc.perform(post("/api/v1/boards")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.name").value("Nusa Bioscience"));
    }

    @Test
    @DisplayName("GIVEN request WHEN GET /api/v1/boards/cards THEN returns 200 with meta")
    void getBoardCardsShouldReturn200WithMeta() throws Exception {
        var meta = new BoardPageMetaDto(
            1, null,
            new BoardPageMetaDto.BoardSummaryMetaDto(1, 1, 0),
            List.of(new BoardPageMetaDto.BoardGroupFilterChipDto("all", "All", 1)));
        var card = new BoardCardResponse(
            UUID.randomUUID(), "NB", "Nusa Bioscience", null, null,
            "on_track", null, 0,
            new BoardTaskCountsDto(0, 0, 0, 0),
            0, 0, 0, LocalDate.now().plusDays(10), 10L, List.of(), 0, "\"1\"");

        given(boardService.getBoardCards(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
            .willReturn(RestResponse.ok(List.of(card), meta, "Berhasil"));

        mockMvc.perform(get("/api/v1/boards/cards?tab=mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.meta.summary.onTrack").value(1));
    }

    @Test
    @DisplayName("GIVEN valid If-Match WHEN DELETE /api/v1/boards/{id} THEN archives board successfully")
    void deleteBoardShouldArchiveSuccessfully() throws Exception {
        UUID boardId = UUID.randomUUID();

        mockMvc
            .perform(MockMvcRequestBuilders.delete("/api/v1/boards/{boardId}", boardId)
                .header(HttpHeaders.IF_MATCH, "\"12345\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GIVEN request WHEN GET /api/v1/boards/{id}/members THEN returns 200 with members and pending invitations")
    void getBoardMembersShouldReturn200() throws Exception {
        UUID boardId = UUID.randomUUID();
        BoardMemberListResponse response = new BoardMemberListResponse(List.of(), List.of());

        given(boardService.getBoardMembers(any(), eq(boardId))).willReturn(response);

        mockMvc.perform(get("/api/v1/boards/{boardId}/members", boardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.data").isArray())
            .andExpect(jsonPath("$.data.pendingInvitations").isArray());
    }

    @Test
    @DisplayName("GIVEN valid payload WHEN POST /api/v1/boards/{id}/invitations THEN returns 200 OK")
    void inviteMembersShouldReturn200() throws Exception {
        UUID boardId = UUID.randomUUID();
        var req = new InviteBoardMembersRequest(List.of("budi@contoh.id"), "member", List.of());
        given(boardService.inviteMembers(any(), eq(boardId), any()))
            .willReturn(new BatchInviteResultResponse(List.of()));

        mockMvc.perform(post("/api/v1/boards/{boardId}/invitations", boardId)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GIVEN valid payload and If-Match WHEN PATCH /api/v1/boards/{boardId}/members/{memberId} THEN returns 200 OK")
    void updateBoardMemberShouldReturn200() throws Exception {
        UUID boardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        var req = new UpdateBoardMemberRequest("admin", List.of());
        var res = new BoardMemberItemDto(memberId, null, "admin", false, List.of(), "\"100\"");

        given(boardService.updateBoardMember(any(), eq(boardId), eq(memberId), any(), eq("\"100\""))).willReturn(res);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/boards/{boardId}/members/{memberId}", boardId, memberId)
            .header(HttpHeaders.IF_MATCH, "\"100\"")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test
    @DisplayName("GIVEN valid If-Match WHEN DELETE /api/v1/boards/{boardId}/members/{memberId} THEN returns 200 OK")
    void removeBoardMemberShouldReturn200() throws Exception {
        UUID boardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/boards/{boardId}/members/{memberId}", boardId, memberId)
            .header(HttpHeaders.IF_MATCH, "\"100\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GIVEN valid boardId WHEN GET /api/v1/boards/{boardId}/views THEN returns 200 OK")
    void getBoardSavedViewsShouldReturn200() throws Exception {
        UUID boardId = UUID.randomUUID();
        SavedViewResponse view = new SavedViewResponse(
            UUID.randomUUID(), "My View", "list", java.util.Map.of(), java.util.Map.of(), null, true, 1, "\"100\"");
        given(boardService.getSavedViews(any(), eq(boardId))).willReturn(List.of(view));

        mockMvc.perform(get("/api/v1/boards/{boardId}/views", boardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data[0].name").value("My View"));
    }

    @Test
    @DisplayName("GIVEN valid payload WHEN POST /api/v1/boards/{boardId}/views THEN returns 201 Created")
    void createBoardSavedViewShouldReturn201() throws Exception {
        UUID boardId = UUID.randomUUID();
        var req = new CreateSavedViewRequest("My Custom View", "list", java.util.Map.of(), java.util.Map.of(), null,
            false);
        SavedViewResponse res = new SavedViewResponse(
            UUID.randomUUID(), "My Custom View", "list", java.util.Map.of(), java.util.Map.of(), null, false, 2,
            "\"100\"");
        given(boardService.createSavedView(any(), eq(boardId), any())).willReturn(res);

        mockMvc.perform(post("/api/v1/boards/{boardId}/views", boardId)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.name").value("My Custom View"));
    }

    @Test
    @DisplayName("GIVEN request to /workflow-templates WHEN requested THEN returns 200 OK with templates list")
    void getWorkflowTemplatesShouldReturn200() throws Exception {
        var template = new WorkflowTemplateResponse(
            UUID.randomUUID(),
            "Default Workflow",
            "Standar 4 kolom",
            true,
            List.of(new WorkflowTemplateResponse.WorkflowTemplateColumnDto("Backlog", "not_started", "#1E293B")));
        given(boardService.getWorkflowTemplates(any())).willReturn(List.of(template));

        mockMvc.perform(get("/api/v1/boards/workflow-templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data[0].name").value("Default Workflow"))
            .andExpect(jsonPath("$.data[0].columns[0].name").value("Backlog"));
    }
}
