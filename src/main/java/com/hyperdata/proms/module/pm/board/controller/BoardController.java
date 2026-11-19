package com.hyperdata.proms.module.pm.board.controller;

import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.pm.board.service.BoardService;
import com.hyperdata.proms.module.pm.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.client.dto.TaskSummaryDto;
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
}
