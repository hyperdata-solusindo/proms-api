package com.hyperdata.proms.module.work.board.controller;

import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.work.board.dto.BatchInviteResultResponse;
import com.hyperdata.proms.module.work.board.dto.BoardCardResponse;
import com.hyperdata.proms.module.work.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.work.board.dto.BoardMemberItemDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberListResponse;
import com.hyperdata.proms.module.work.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.work.board.dto.CreateBoardRequest;
import com.hyperdata.proms.module.work.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.work.board.dto.CreateSavedViewRequest;
import com.hyperdata.proms.module.work.board.dto.InviteBoardMembersRequest;
import com.hyperdata.proms.module.work.board.dto.SavedViewResponse;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardMemberRequest;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.work.board.dto.WorkflowTemplateResponse;
import com.hyperdata.proms.module.work.board.service.BoardService;
import com.hyperdata.proms.module.work.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskSummaryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardController implements BoardApi {

    private final BoardService boardService;

    @Override
    public RestResponse<BoardDetailResponse> createBoard(UUID idempotencyKey, CreateBoardRequest request) {
        CreateBoardRequest.VALIDATOR.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.created(boardService.createBoard(currentUser, request),
            Messages.get("board.created.success"));
    }

    @Override
    public RestResponse<List<BoardCardResponse>> getBoardCards(
        String tab,
        String groupId,
        String q,
        String cursor,
        int limit) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return boardService.getBoardCards(currentUser, tab, groupId, q, cursor, limit);
    }

    @Override
    public RestResponse<Void> deleteBoard(UUID boardId, String ifMatch, boolean permanent) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        boardService.archiveBoard(boardId, currentUser, ifMatch);
        return RestResponse.ok(null, Messages.get("board.archived.success"));
    }

    @Override
    public RestResponse<PageResponse<BoardSummaryResponse>> listBoards(String tab, String q, Pageable pageable) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.listBoards(currentUser, tab, q, pageable),
            Messages.get("board.list.retrieved"));
    }

    @Override
    public RestResponse<BoardDetailResponse> getBoardDetail(UUID boardId) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.getBoardDetail(boardId, currentUser),
            Messages.get("board.detail.retrieved"));
    }

    @Override
    public RestResponse<BoardDetailResponse> updateBoard(UUID boardId, UpdateBoardRequest request) {
        UpdateBoardRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.updateBoard(boardId, currentUser, request),
            Messages.get("board.updated.success"));
    }

    @Override
    public RestResponse<PageResponse<TaskSummaryDto>> getBoardTasks(UUID boardId, BoardTaskFilterDto filter) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.getBoardTasks(boardId, currentUser, filter),
            "Berhasil mengambil tasks board");
    }

    @Override
    public RestResponse<TaskCreatedDto> createBoardTask(UUID boardId, CreateBoardTaskRequest request) {
        CreateBoardTaskRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.created(boardService.createBoardTask(boardId, currentUser, request),
            "Task berhasil dibuat");
    }

    @Override
    public RestResponse<List<ChatChannelSummaryDto>> getBoardChatChannels(UUID boardId) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.getBoardChatChannels(boardId, currentUser),
            "Berhasil mengambil channel chat board");
    }

    @Override
    public RestResponse<BoardMemberListResponse> getBoardMembers(UUID boardId) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.getBoardMembers(currentUser, boardId),
            "Berhasil mengambil daftar anggota board");
    }

    @Override
    public RestResponse<BatchInviteResultResponse> inviteBoardMembers(
        UUID boardId,
        UUID idempotencyKey,
        InviteBoardMembersRequest request) {
        InviteBoardMembersRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.inviteMembers(currentUser, boardId, request),
            "Undangan berhasil diproses");
    }

    @Override
    public RestResponse<BoardMemberItemDto> updateBoardMember(
        UUID boardId,
        UUID memberId,
        String ifMatch,
        UpdateBoardMemberRequest request) {
        UpdateBoardMemberRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.updateBoardMember(currentUser, boardId, memberId, request, ifMatch),
            "Data anggota berhasil diperbarui");
    }

    @Override
    public RestResponse<Void> removeBoardMember(UUID boardId, UUID memberId, String ifMatch) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        boardService.removeMember(currentUser, boardId, memberId, ifMatch);
        return RestResponse.ok(null, "Anggota berhasil dikeluarkan dari board");
    }

    @Override
    public RestResponse<List<SavedViewResponse>> getBoardSavedViews(UUID boardId) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.getSavedViews(currentUser, boardId));
    }

    @Override
    public RestResponse<SavedViewResponse> createBoardSavedView(
        UUID boardId,
        UUID idempotencyKey,
        CreateSavedViewRequest request) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        SavedViewResponse response = boardService.createSavedView(currentUser, boardId, request);
        return RestResponse.created(response, "Saved view berhasil dibuat");
    }

    @Override
    public RestResponse<List<WorkflowTemplateResponse>> getWorkflowTemplates() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(boardService.getWorkflowTemplates(currentUser), "Template workflow berhasil diambil");
    }
}
