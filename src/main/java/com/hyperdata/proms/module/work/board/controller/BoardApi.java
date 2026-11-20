package com.hyperdata.proms.module.work.board.controller;

import com.hyperdata.proms.common.docs.annotation.ApiConcurrencyControl;
import com.hyperdata.proms.common.docs.annotation.ApiResponseNotFound;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.work.board.constant.BoardTab;
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
import com.hyperdata.proms.module.work.client.dto.BoardTaskFilterDto;
import com.hyperdata.proms.module.work.client.dto.TaskCreatedDto;
import com.hyperdata.proms.module.work.client.dto.TaskSummaryDto;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Boards", description = "Manajemen data project boards, kanban columns, dan board tasks")
@RequestMapping("/api/v1/boards")
public interface BoardApi {

    @Operation(
        summary = "Buat Board Baru",
        description = "Membuat board baru dan menginisialisasi kolom dari template alur kerja.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<BoardDetailResponse> createBoard(
        @Parameter(description = "Idempotency key untuk mencegah duplikasi pembuatan", required = false)
        @RequestHeader(value = "Idempotency-Key", required = false)
        UUID idempotencyKey,
        @RequestBody
        CreateBoardRequest request);

    @Operation(
        summary = "Card View Boards",
        description = "Mengambil representasi visual kartu board beserta metrik task, days left, chip filter grup, dan avatar anggota.")
    @GetMapping("/cards")
    RestResponse<List<BoardCardResponse>> getBoardCards(
        @Parameter(description = "Filter tab: mine, shared, archived", example = BoardTab.MINE)
        @RequestParam(value = "tab", required = false, defaultValue = BoardTab.MINE)
        String tab,
        @Parameter(description = "Filter berdasarkan nama atau UUID grup divisi")
        @RequestParam(value = "groupId", required = false)
        String groupId,
        @Parameter(description = "Keyword pencarian nama atau kode board")
        @RequestParam(value = "q", required = false)
        String q,
        @Parameter(description = "Kursor paginasi")
        @RequestParam(value = "cursor", required = false)
        String cursor,
        @Parameter(description = "Ukuran limit data", example = "20")
        @RequestParam(value = "limit", required = false, defaultValue = "20")
        int limit);

    @Operation(
        summary = "Arsip / Hapus Board",
        description = "Mengarsipkan board secara logis dengan proteksi concurrency If-Match.")
    @ApiResponse(responseCode = "200", description = "Board berhasil diarsipkan")
    @ApiResponseNotFound
    @ApiConcurrencyControl
    @DeleteMapping("/{boardId}")
    RestResponse<Void> deleteBoard(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @Parameter(description = "Versi timestamp/epoch board saat ini", required = true)
        @RequestHeader(value = "If-Match")
        String ifMatch,
        @Parameter(description = "Flag hapus permanen (reserved)", required = false)
        @RequestParam(value = "permanent", required = false, defaultValue = "false")
        boolean permanent);

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
        @ParameterObject
        @PageableDefault(size = 20)
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
        @ParameterObject
        @ModelAttribute
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

    @Operation(
        summary = "Daftar Anggota Board",
        description = "Mengambil daftar anggota board aktif dan undangan tertunda")
    @GetMapping("/{boardId}/members")
    RestResponse<BoardMemberListResponse> getBoardMembers(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId);

    @Operation(summary = "Undang Anggota ke Board", description = "Mengundang anggota ke board via email secara batch")
    @PostMapping("/{boardId}/invitations")
    RestResponse<BatchInviteResultResponse> inviteBoardMembers(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @Parameter(description = "Idempotency key untuk mencegah pengiriman ganda", required = false)
        @RequestHeader(value = "Idempotency-Key", required = false)
        UUID idempotencyKey,
        @RequestBody
        InviteBoardMembersRequest request);

    @Operation(
        summary = "Perbarui Role/Workstream Anggota",
        description = "Memperbarui peran atau alokasi workstream anggota board dengan If-Match guard")
    @ApiConcurrencyControl
    @PatchMapping("/{boardId}/members/{memberId}")
    RestResponse<BoardMemberItemDto> updateBoardMember(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @Parameter(description = "UUID Member Board", required = true)
        @PathVariable("memberId")
        UUID memberId,
        @Parameter(description = "Versi timestamp/epoch member saat ini", required = true)
        @RequestHeader("If-Match")
        String ifMatch,
        @RequestBody
        UpdateBoardMemberRequest request);

    @Operation(
        summary = "Keluarkan Anggota dari Board",
        description = "Mengeluarkan anggota dan melepaskan seluruh penugasan task pada board terkait (BR-DL-04)")
    @ApiConcurrencyControl
    @DeleteMapping("/{boardId}/members/{memberId}")
    RestResponse<Void> removeBoardMember(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @Parameter(description = "UUID Member Board", required = true)
        @PathVariable("memberId")
        UUID memberId,
        @Parameter(description = "Versi timestamp/epoch member saat ini", required = true)
        @RequestHeader("If-Match")
        String ifMatch);

    @Operation(
        summary = "Daftar Saved Views Board",
        description = "Mengambil daftar preset filter view tersimpan pada board")
    @GetMapping("/{boardId}/views")
    RestResponse<List<SavedViewResponse>> getBoardSavedViews(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId);

    @Operation(
        summary = "Simpan Custom View Baru",
        description = "Menyimpan preset filter/sort baru sebagai view kustom")
    @PostMapping("/{boardId}/views")
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<SavedViewResponse> createBoardSavedView(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId")
        UUID boardId,
        @Parameter(description = "Idempotency key untuk mencegah duplikasi pembuatan", required = false)
        @RequestHeader(value = "Idempotency-Key", required = false)
        UUID idempotencyKey,
        @RequestBody
        CreateSavedViewRequest request);

    @Operation(
        summary = "Daftar Template Alur Kerja",
        description = "Mengambil daftar preset template alur kerja (workflow templates) beserta susunan kolom kanban untuk inisiasi board.")
    @GetMapping(path = {"/workflow-templates", "/api/v1/workflow-templates"})
    RestResponse<List<WorkflowTemplateResponse>> getWorkflowTemplates();
}
