package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.dto.BoardMemberCountDto;
import com.hyperdata.proms.module.pm.board.dto.BoardMemberRoleDto;
import com.hyperdata.proms.module.pm.board.entity.BoardMember;
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
            SELECT new com.hyperdata.proms.module.pm.board.dto.BoardMemberCountDto(bm.boardId, COUNT(bm.id))
            FROM BoardMember bm
            WHERE bm.boardId IN :boardIds
            GROUP BY bm.boardId
        """)
    List<BoardMemberCountDto> countMembersGroupedByBoardIds(@Param("boardIds")
    Collection<UUID> boardIds);

    @Query("""
            SELECT new com.hyperdata.proms.module.pm.board.dto.BoardMemberRoleDto(bm.boardId, bm.role)
            FROM BoardMember bm
            WHERE bm.userId = :userId AND bm.boardId IN :boardIds
        """)
    List<BoardMemberRoleDto> findRolesByUserAndBoardIds(
        @Param("userId")
        UUID userId,
        @Param("boardIds")
        Collection<UUID> boardIds);
}
