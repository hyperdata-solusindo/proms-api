package com.hyperdata.proms.module.work.task.controller;

import com.hyperdata.proms.common.docs.annotation.ApiConcurrencyControl;
import com.hyperdata.proms.common.docs.annotation.ApiResponseNotFound;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.work.task.dto.MoveTaskRequest;
import com.hyperdata.proms.module.work.task.dto.MyTaskFilter;
import com.hyperdata.proms.module.work.task.dto.MyTaskSummaryResponse;
import com.hyperdata.proms.module.work.task.dto.TaskActivityResponse;
import com.hyperdata.proms.module.work.task.dto.TaskDetailResponse;
import com.hyperdata.proms.module.work.task.dto.TaskListItemResponse;
import com.hyperdata.proms.module.work.task.dto.UpdateTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Tasks", description = "Task saya, detail task, pembaruan, pemindahan kolom, dan riwayat aktivitas")
@RequestMapping("/api/v1")
public interface TaskApi {

    @Operation(
        summary = "Daftar Task Saya",
        description = "API-04. Task aktif yang di-assign ke user, urut due date terdekat. Segmen due dihitung dengan "
            + "zona waktu tenant. Cursor halaman berikutnya ada di meta.nextCursor.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daftar task berhasil diambil"),
        @ApiResponse(
            responseCode = "404",
            description = "BOARD_NOT_FOUND: filter boardId bukan board yang bisa diakses user",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/me/tasks")
    RestResponse<List<TaskListItemResponse>> getMyTasks(
        @ParameterObject
        @ModelAttribute
        MyTaskFilter filter);

    @Operation(
        summary = "Ringkasan Task Saya",
        description = "Jumlah task aktif saya per segmen (hari ini, terlambat, minggu ini, semua) untuk kartu Home. "
            + "Task di kolom done tidak dihitung.")
    @ApiResponse(responseCode = "200", description = "Ringkasan berhasil diambil")
    @GetMapping("/me/tasks/summary")
    RestResponse<MyTaskSummaryResponse> getMyTaskSummary();

    @Operation(
        summary = "Detail Task",
        description = "API-15. Task lengkap beserta subtask, checklist, dependency, dan permission user aktif.")
    @ApiResponse(responseCode = "200", description = "Detail task berhasil diambil")
    @ApiResponseNotFound
    @GetMapping("/tasks/{taskId}")
    RestResponse<TaskDetailResponse> getTaskDetail(
        @Parameter(description = "UUID task", required = true)
        @PathVariable("taskId")
        UUID taskId);

    @Operation(
        summary = "Detail Task via Key",
        description = "API-16. Membuka task dari kode {boardKey}-{number}, misalnya dari deep link atau mention chat.")
    @ApiResponse(responseCode = "200", description = "Detail task berhasil diambil")
    @ApiResponseNotFound
    @GetMapping("/task-keys/{taskKey}")
    RestResponse<TaskDetailResponse> getTaskByKey(
        @Parameter(description = "Kode task", example = "NB-12", required = true)
        @PathVariable("taskKey")
        String taskKey);

    @Operation(
        summary = "Perbarui Task",
        description = "API-17. Pembaruan sebagian: field null tidak diubah, assigneeIds adalah daftar lengkap "
            + "pengganti. Setiap field yang berubah dicatat ke riwayat task.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task berhasil diperbarui"),
        @ApiResponse(
            responseCode = "403",
            description = "FORBIDDEN_TASK_EDIT: viewer/guest tidak boleh mengubah task",
            content = @Content(schema = @Schema(implementation = RestResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "VALIDATION: payload tidak valid atau assignee bukan anggota board",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @ApiResponseNotFound
    @ApiConcurrencyControl
    @PatchMapping("/tasks/{taskId}")
    RestResponse<TaskDetailResponse> updateTask(
        @Parameter(description = "UUID task", required = true)
        @PathVariable("taskId")
        UUID taskId,
        @Parameter(hidden = true)
        @RequestHeader(value = "If-Match", required = false)
        String ifMatch,
        @RequestBody
        UpdateTaskRequest request);

    @Operation(
        summary = "Pindahkan Task ke Kolom",
        description = "API-18. Memindah task ke kolom lain dan menaruhnya di akhir kolom. Urutan cek: hak pindah "
            + "(BR-WF-06), kolom done hanya PM/admin/owner (BR-WF-07), alasan wajib untuk kolom blocked (BR-WF-09).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task berhasil dipindah"),
        @ApiResponse(
            responseCode = "403",
            description = "BR-WF-06 tidak berhak memindah task; BR-WF-07 tidak berhak memindah ke kolom done",
            content = @Content(schema = @Schema(implementation = RestResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "BR-WF-09: alasan wajib diisi untuk kolom blocked",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @ApiResponseNotFound
    @ApiConcurrencyControl
    @PatchMapping("/tasks/{taskId}/move")
    RestResponse<TaskDetailResponse> moveTask(
        @Parameter(description = "UUID task", required = true)
        @PathVariable("taskId")
        UUID taskId,
        @Parameter(hidden = true)
        @RequestHeader(value = "If-Match", required = false)
        String ifMatch,
        @RequestBody
        MoveTaskRequest request);

    @Operation(
        summary = "Riwayat Aktivitas Task",
        description = "API-22. Riwayat perubahan task, terbaru dulu. Cursor halaman berikutnya ada di meta.nextCursor.")
    @ApiResponse(responseCode = "200", description = "Riwayat aktivitas berhasil diambil")
    @ApiResponseNotFound
    @GetMapping("/tasks/{taskId}/activities")
    RestResponse<List<TaskActivityResponse>> getTaskActivities(
        @Parameter(description = "UUID task", required = true)
        @PathVariable("taskId")
        UUID taskId,
        @Parameter(description = "Cursor dari meta.nextCursor halaman sebelumnya")
        @RequestParam(value = "cursor", required = false)
        String cursor,
        @Parameter(description = "Jumlah item per halaman (maks 200)", example = "50")
        @RequestParam(value = "limit", required = false)
        Integer limit);
}
