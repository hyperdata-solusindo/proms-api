package com.hyperdata.proms.module.pm.board.repository;

import com.hyperdata.proms.module.pm.board.entity.Board;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, UUID>, JpaSpecificationExecutor<Board> {

    @Query("""
            SELECT b FROM Board b
            WHERE b.id = :id
              AND b.deletedAt IS NULL
        """)
    Optional<Board> findActiveById(@Param("id")
    UUID id);
}
