package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.dto.BoardMemberCountDto;
import com.hyperdata.proms.module.work.board.dto.BoardMemberRoleDto;
import com.hyperdata.proms.module.work.board.entity.BoardMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {

    Optional<BoardMember> findByBoardIdAndUserId(UUID boardId, UUID userId);

    List<BoardMember> findByBoardId(UUID boardId);

    @Query("""
            SELECT new com.hyperdata.proms.module.work.board.dto.BoardMemberCountDto(bm.boardId, COUNT(bm.id))
            FROM BoardMember bm
            WHERE bm.boardId IN :boardIds
            GROUP BY bm.boardId
        """)
    List<BoardMemberCountDto> countMembersGroupedByBoardIds(@Param("boardIds")
    Collection<UUID> boardIds);

    @Query("""
            SELECT new com.hyperdata.proms.module.work.board.dto.BoardMemberRoleDto(bm.boardId, bm.role)
            FROM BoardMember bm
            WHERE bm.userId = :userId AND bm.boardId IN :boardIds
        """)
    List<BoardMemberRoleDto> findRolesByUserAndBoardIds(
        @Param("userId")
        UUID userId,
        @Param("boardIds")
        Collection<UUID> boardIds);

    @Query("""
            SELECT bm FROM BoardMember bm
            WHERE bm.boardId IN :boardIds
            ORDER BY bm.createdAt ASC
        """)
    List<BoardMember> findMembersByBoardIds(@Param("boardIds")
    Collection<UUID> boardIds);

    @Query("""
            SELECT bm FROM BoardMember bm
            WHERE bm.id = :id AND bm.boardId = :boardId AND bm.tenantId = :tenantId
        """)
    Optional<BoardMember> findByIdAndBoardIdAndTenantId(
        @Param("id")
        UUID id,
        @Param("boardId")
        UUID boardId,
        @Param("tenantId")
        UUID tenantId);
}
