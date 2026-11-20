package com.hyperdata.proms.module.work.board.repository;

import com.hyperdata.proms.module.work.board.entity.BoardMemberWorkstream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardMemberWorkstreamRepository
    extends
        JpaRepository<BoardMemberWorkstream, BoardMemberWorkstream.BoardMemberWorkstreamId> {

    @Query("""
            SELECT bmw FROM BoardMemberWorkstream bmw
            WHERE bmw.boardMemberId IN :memberIds
        """)
    List<BoardMemberWorkstream> findByBoardMemberIdIn(@Param("memberIds")
    Collection<UUID> memberIds);

    @Query("""
            SELECT bmw FROM BoardMemberWorkstream bmw
            WHERE bmw.boardMemberId = :memberId
        """)
    List<BoardMemberWorkstream> findByBoardMemberId(@Param("memberId")
    UUID memberId);

    @Modifying
    @Query("""
            DELETE FROM BoardMemberWorkstream bmw
            WHERE bmw.boardMemberId = :memberId
        """)
    void deleteByBoardMemberId(@Param("memberId")
    UUID memberId);
}
