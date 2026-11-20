package com.hyperdata.proms.module.work.task.repository.spec;

import com.hyperdata.proms.module.work.task.entity.Task;
import com.hyperdata.proms.module.work.task.entity.TaskAssignees;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpec {

    private TaskSpec() {
    }

    public static Specification<Task> isActive() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Task> inBoard(UUID boardId) {
        return (root, query, cb) -> cb.equal(root.get("boardId"), boardId);
    }

    /** Batasi ke board yang boleh dilihat user (mis. board aktif tempat user menjadi member). */
    public static Specification<Task> inBoards(Collection<UUID> boardIds) {
        return (root, query, cb) -> boardIds.isEmpty() ? cb.disjunction() : root.get("boardId").in(boardIds);
    }

    /** Task yang di-assign ke user (subquery ke task_assignees milik fitur task). */
    public static Specification<Task> assignedTo(UUID userId) {
        return (root, query, cb) -> {
            Subquery<UUID> assigned = query.subquery(UUID.class);
            var assignee = assigned.from(TaskAssignees.class);
            assigned.select(assignee.get("id").get("taskId"))
                .where(cb.equal(assignee.get("id").get("userId"), userId));
            return root.get("id").in(assigned);
        };
    }

    public static Specification<Task> inWorkstream(UUID workstreamId) {
        return (root, query, cb) -> cb.equal(root.get("workstreamId"), workstreamId);
    }

    public static Specification<Task> hasPriority(String priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    /** Filter status: kolom dengan kategori tertentu (ID kolom di-resolve dari BoardClient). */
    public static Specification<Task> inColumns(Collection<UUID> columnIds) {
        return (root, query, cb) -> columnIds.isEmpty() ? cb.disjunction() : root.get("columnId").in(columnIds);
    }

    /** Kecualikan kolom tertentu, mis. kolom done saat {@code includeDone = false}. */
    public static Specification<Task> notInColumns(Collection<UUID> columnIds) {
        return (root, query, cb) -> columnIds.isEmpty()
            ? cb.conjunction()
            : cb.not(root.get("columnId").in(columnIds));
    }

    /** Segmen {@code today}. */
    public static Specification<Task> dueOn(LocalDate date) {
        return (root, query, cb) -> cb.equal(root.get("dueDate"), date);
    }

    /** Segmen {@code overdue}: jatuh tempo sebelum tanggal hari ini (tenant). */
    public static Specification<Task> dueBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThan(root.get("dueDate"), date);
    }

    /** Segmen {@code upcoming}/minggu ini: rentang tanggal inklusif. */
    public static Specification<Task> dueBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get("dueDate"), from, to);
    }

    /** Pencarian kata kunci pada judul dan teks deskripsi. */
    public static Specification<Task> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("descriptionText")), pattern));
        };
    }

    /** Guest hanya melihat task yang dibagikan ke klien. */
    public static Specification<Task> sharedWithClient() {
        return (root, query, cb) -> cb.isTrue(root.get("sharedWithClient"));
    }

    /** Hanya task induk (bukan subtask). */
    public static Specification<Task> topLevel() {
        return (root, query, cb) -> cb.isNull(root.get("parentId"));
    }
}
