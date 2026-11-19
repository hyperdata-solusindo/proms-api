package com.hyperdata.proms.module.work.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_member_workstreams", schema = "proms")
@IdClass(BoardMemberWorkstream.BoardMemberWorkstreamId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberWorkstream {

    @Id
    @Column(name = "board_member_id", nullable = false)
    private UUID boardMemberId;

    @Id
    @Column(name = "workstream_id", nullable = false)
    private UUID workstreamId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class BoardMemberWorkstreamId implements Serializable {

        private UUID boardMemberId;
        private UUID workstreamId;
    }
}
