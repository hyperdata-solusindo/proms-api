package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardSavedViewRepository extends JpaRepository<BoardSavedView, UUID> {

    @Query("""
            SELECT v FROM BoardSavedView v
            WHERE v.tenantId = :tenantId
              AND v.boardId = :boardId
              AND (v.userId = :userId OR v.isDefault = true)
            ORDER BY v.position ASC
        """)
    List<BoardSavedView> findByTenantIdAndBoardIdAndUserId(
        @Param("tenantId")
        UUID tenantId,
        @Param("boardId")
        UUID boardId,
        @Param("userId")
        UUID userId);

    @Query("""
            SELECT COALESCE(MAX(v.position), 0) FROM BoardSavedView v
            WHERE v.tenantId = :tenantId
              AND v.boardId = :boardId
              AND v.userId = :userId
        """)
    int findMaxPosition(
        @Param("tenantId")
        UUID tenantId,
        @Param("boardId")
        UUID boardId,
        @Param("userId")
        UUID userId);

    @Modifying
    @Query("""
            UPDATE BoardSavedView v
            SET v.isDefault = false
            WHERE v.tenantId = :tenantId
              AND v.boardId = :boardId
              AND v.userId = :userId
              AND v.isDefault = true
        """)
    void resetDefaultFlag(
        @Param("tenantId")
        UUID tenantId,
        @Param("boardId")
        UUID boardId,
        @Param("userId")
        UUID userId);
}
