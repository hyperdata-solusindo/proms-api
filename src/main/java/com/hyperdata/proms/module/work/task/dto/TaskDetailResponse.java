package com.hyperdata.proms.module.work.task.dto;

import com.hyperdata.proms.common.docs.annotation.VersionEpochSchema;
import com.hyperdata.proms.module.work.client.dto.TaskUserDto;
import com.hyperdata.proms.module.work.task.constant.TaskCategory;
import com.hyperdata.proms.module.work.task.constant.TaskDueDate;
import com.hyperdata.proms.module.work.task.constant.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Detail lengkap task termasuk subtask, checklist, dependency, lampiran, dan permission")
public record TaskDetailResponse(
    @Schema(description = "UUID task", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c01")
    UUID id,
    @Schema(description = "Kode task {boardKey}-{number}", example = "NB-12")
    String key,
    @Schema(description = "UUID board", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c70")
    UUID boardId,
    @Schema(description = "Nama board", example = "Nusa Bioscience")
    String boardName,
    @Schema(description = "UUID workstream", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c7d")
    UUID workstreamId,
    @Schema(description = "UUID kolom saat ini", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c80")
    UUID columnId,
    @Schema(description = "Kategori kolom", example = TaskCategory.IN_PROGRESS)
    String category,
    @Schema(description = "UUID task induk bila ini subtask")
    UUID parentId,
    @Schema(description = "Judul task", example = "Integrasi sensor suhu")
    String title,
    @Schema(description = "Dokumen editor deskripsi (JSON)")
    Map<String, Object> description,
    @Schema(description = "Prioritas", example = TaskPriority.HIGH)
    String priority,
    @Schema(description = "Waktu mulai", example = "2026-10-20T02:00:00Z", format = "date-time")
    Instant startDate,
    @Schema(description = "Tanggal jatuh tempo", example = "2026-10-28", format = "date")
    LocalDate dueDate,
    @Schema(description = "Status jatuh tempo dihitung server", example = TaskDueDate.SOON)
    String dueState,
    @Schema(description = "Jumlah hari terlambat (0 bila tidak terlambat)", example = "0")
    int daysLate,
    @Schema(description = "UUID milestone")
    UUID milestoneId,
    @Schema(description = "Task sedang blocked", example = "false")
    boolean isBlocked,
    @Schema(description = "Alasan blocked", example = "Menunggu suplai sensor")
    String blockedReason,
    @Schema(description = "Waktu masuk kolom done (diisi trigger)", format = "date-time")
    Instant completedAt,
    @Schema(description = "Terlihat oleh guest/klien", example = "false")
    boolean sharedWithClient,
    @Schema(description = "Assignee task")
    List<TaskUserDto> assignees,
    @Schema(description = "Subtask (task dengan parentId = task ini)")
    List<SubtaskResponse> subtasks,
    @Schema(description = "Checklist, urut position")
    List<ChecklistItemResponse> checklist,
    @Schema(description = "Relasi dependency task")
    DependenciesResponse dependencies,
    @Schema(description = "Lampiran task (dari modul attachment)")
    List<AttachmentResponse> attachments,
    @Schema(description = "Hak aksi user aktif atas task ini")
    TaskPermissionResponse permissions,
    @Schema(description = "Pembuat task")
    TaskUserDto createdBy,
    @Schema(description = "Waktu dibuat", format = "date-time")
    Instant createdAt,
    @VersionEpochSchema
    Instant version) {

    @Schema(description = "Ringkasan subtask")
    public record SubtaskResponse(
        @Schema(description = "UUID subtask")
        UUID id,
        @Schema(description = "Kode subtask", example = "NB-13")
        String key,
        @Schema(description = "Judul subtask", example = "Cek konektor")
        String title,
        @Schema(description = "Kategori kolom", example = TaskCategory.DONE)
        String category,
        @Schema(description = "Subtask sudah selesai", example = "true")
        boolean isDone) {
    }

    @Schema(description = "Item checklist")
    public record ChecklistItemResponse(
        @Schema(description = "UUID item checklist")
        UUID id,
        @Schema(description = "Teks item", example = "Kalibrasi")
        String title,
        @Schema(description = "Status selesai", example = "true")
        boolean isDone,
        @Schema(description = "Urutan item", example = "0")
        int position) {
    }

    @Schema(description = "Dependency task: yang memblokir task ini dan yang diblokir task ini")
    public record DependenciesResponse(
        @Schema(description = "Task yang harus selesai dulu")
        List<TaskRefResponse> blockedBy,
        @Schema(description = "Task yang menunggu task ini")
        List<TaskRefResponse> blocking) {
    }

    @Schema(description = "Referensi ringkas task lain")
    public record TaskRefResponse(
        @Schema(description = "UUID task")
        UUID id,
        @Schema(description = "Kode task", example = "NB-9")
        String key,
        @Schema(description = "Judul task", example = "Pengadaan sensor")
        String title,
        @Schema(description = "Kategori kolom", example = TaskCategory.DONE)
        String category) {
    }

    @Schema(description = "Ringkasan lampiran")
    public record AttachmentResponse(
        @Schema(description = "UUID lampiran")
        UUID id,
        @Schema(description = "Nama file", example = "wiring-diagram.pdf")
        String fileName,
        @Schema(description = "MIME type", example = "application/pdf")
        String mimeType,
        @Schema(description = "Ukuran file dalam byte (maks 25 MB)", example = "482133")
        long sizeBytes,
        @Schema(description = "Pengunggah")
        TaskUserDto uploadedBy,
        @Schema(description = "Waktu unggah", format = "date-time")
        Instant createdAt) {
    }

    @Schema(description = "Permission aksi task untuk menyembunyikan tombol di app")
    public record TaskPermissionResponse(
        @Schema(description = "Boleh mengubah task", example = "true")
        boolean canEdit,
        @Schema(description = "Boleh memindah ke kolom done (BR-WF-07)", example = "false")
        boolean canMoveToDone,
        @Schema(description = "Boleh mengubah due date", example = "true")
        boolean canEditDueDate) {
    }
}
