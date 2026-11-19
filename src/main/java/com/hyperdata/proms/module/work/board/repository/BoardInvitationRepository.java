package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.BoardInvitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardInvitationRepository extends JpaRepository<BoardInvitation, UUID> {

    @Query("""
            SELECT bi FROM BoardInvitation bi
            WHERE bi.tenantId = :tenantId
              AND bi.boardId = :boardId
              AND bi.acceptedAt IS NULL
              AND bi.revokedAt IS NULL
              AND bi.expiresAt > :now
            ORDER BY bi.createdAt DESC
        """)
    List<BoardInvitation> findPendingByBoardId(
        @Param("tenantId")
        UUID tenantId,
        @Param("boardId")
        UUID boardId,
        @Param("now")
        Instant now);

    @Query("""
            SELECT bi FROM BoardInvitation bi
            WHERE bi.tenantId = :tenantId
              AND bi.boardId = :boardId
              AND bi.email = :email
              AND bi.acceptedAt IS NULL
              AND bi.revokedAt IS NULL
              AND bi.expiresAt > :now
        """)
    Optional<BoardInvitation> findPendingByEmail(
        @Param("tenantId")
        UUID tenantId,
        @Param("boardId")
        UUID boardId,
        @Param("email")
        String email,
        @Param("now")
        Instant now);
}
