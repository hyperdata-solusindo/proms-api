package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.BoardColumn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    @Query("""
            SELECT c FROM BoardColumn c
            WHERE c.boardId = :boardId
              AND c.deletedAt IS NULL
            ORDER BY c.position ASC
        """)
    List<BoardColumn> findActiveColumnsByBoardId(@Param("boardId")
    UUID boardId);

    @Query("""
            SELECT c FROM BoardColumn c
            WHERE c.id = :id
              AND c.boardId = :boardId
              AND c.deletedAt IS NULL
        """)
    Optional<BoardColumn> findActiveColumnById(@Param("id")
    UUID id, @Param("boardId")
    UUID boardId);
}
