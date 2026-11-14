package com.hyperdata.proms.module.pm.board.controller;

import com.hyperdata.proms.common.docs.annotation.ApiConcurrencyControl;
import com.hyperdata.proms.common.docs.annotation.ApiResponseNotFound;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.pm.board.constant.BoardTab;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.dto.CreateBoardTaskRequest;
import com.hyperdata.proms.module.pm.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.pm.task.facade.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskCreatedDto;
import com.hyperdata.proms.module.pm.task.facade.dto.TaskSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Boards", description = "Manajemen data project boards, kanban columns, dan board tasks")
@RequestMapping("/api/v1/boards")
public interface BoardApi {

    @Operation(
        summary = "Daftar Board",
        description = "Mengambil daftar project boards dengan filter tab (mine/shared/archived) dan pencarian keyword.")
    @GetMapping
    RestResponse<PageResponse<BoardSummaryResponse>> listBoards(
        @Parameter(description = "Filter tab: mine, shared, archived", example = BoardTab.MINE)
        @RequestParam(value = "tab", required = false, defaultValue = BoardTab.MINE)
        String tab,
        @Parameter(description = "Keyword pencarian nama atau board key")
        @RequestParam(value = "q", required = false)
        String q,
        @ParameterObject @PageableDefault(size = 20)
        Pageable pageable);

    @Operation(
        summary = "Detail Board",
        description = "Mengambil detail lengkap project board termasuk workstream, columns, members, dan permissions.")
    @ApiResponse(responseCode = "200", description = "Detail board berhasil diambil")
    @ApiResponseNotFound
    @GetMapping("/{boardId}")
    RestResponse<BoardDetailResponse> getBoardDetail(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId);

    @Operation(
        summary = "Update Informasi Board",
        description = "Memperbarui nama dan deskripsi board dengan verifikasi concurrency optimistik melalui header If-Match.")
    @ApiResponse(responseCode = "200", description = "Board berhasil diperbarui")
    @ApiResponseNotFound
    @ApiConcurrencyControl
    @PutMapping("/{boardId}")
    RestResponse<BoardDetailResponse> updateBoard(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @RequestBody
        UpdateBoardRequest request);

    @Operation(
        summary = "Tasks pada Board",
        description = "Mengambil daftar tasks dalam board dengan filter kanban view, workstream, assignee, dsb.")
    @GetMapping("/{boardId}/tasks")
    RestResponse<PageResponse<TaskSummaryDto>> getBoardTasks(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @ParameterObject @ModelAttribute
        BoardTaskFilterDto filter);

    @Operation(
        summary = "Buat Task Baru pada Board",
        description = "Membuat task baru di dalam board pada column dan workstream yang ditentukan.")
    @PostMapping("/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<TaskCreatedDto> createBoardTask(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @RequestBody
        CreateBoardTaskRequest request);

    @Operation(
        summary = "Chat Channels pada Board",
        description = "Mengambil daftar channel chat yang terhubung ke board.")
    @GetMapping("/{boardId}/chat/channels")
    RestResponse<List<ChatChannelSummaryDto>> getBoardChatChannels(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId);
}
