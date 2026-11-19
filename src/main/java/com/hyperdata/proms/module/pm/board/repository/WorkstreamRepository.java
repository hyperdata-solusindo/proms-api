package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.Workstream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkstreamRepository extends JpaRepository<Workstream, UUID> {

    @Query("""
            SELECT w FROM Workstream w
            WHERE w.boardId = :boardId
              AND w.deletedAt IS NULL
            ORDER BY w.position ASC
        """)
    List<Workstream> findActiveWorkstreamsByBoardId(@Param("boardId")
    UUID boardId);

    @Query("""
            SELECT w FROM Workstream w
            WHERE w.id = :id
              AND w.boardId = :boardId
              AND w.deletedAt IS NULL
        """)
    Optional<Workstream> findActiveWorkstreamById(@Param("id")
    UUID id, @Param("boardId")
    UUID boardId);
}
