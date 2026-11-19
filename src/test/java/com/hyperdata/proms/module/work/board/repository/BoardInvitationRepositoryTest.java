package com.hyperdata.proms.module.work.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.entity.BoardInvitation;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BoardInvitationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BoardInvitationRepository invitationRepository;

    @Test
    @DisplayName("Harus dapat mengambil daftar invitation yang masih pending untuk sebuah board")
    void shouldFindPendingInvitationsByBoardId() {
        UUID tenantId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();

        BoardInvitation inv1 = BoardInvitation.builder()
            .tenantId(tenantId)
            .boardId(boardId)
            .email("budi@contoh.id")
            .boardRole(BoardMemberRole.MEMBER)
            .tokenHash("hash123")
            .invitedBy(invitedBy)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        BoardInvitation invExpired = BoardInvitation.builder()
            .tenantId(tenantId)
            .boardId(boardId)
            .email("lama@contoh.id")
            .boardRole(BoardMemberRole.MEMBER)
            .tokenHash("hash456")
            .invitedBy(invitedBy)
            .expiresAt(Instant.now().minusSeconds(3600))
            .build();

        invitationRepository.saveAndFlush(inv1);
        invitationRepository.saveAndFlush(invExpired);
        entityManager.clear();

        var pending = invitationRepository.findPendingByBoardId(tenantId, boardId, Instant.now());
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEmail()).isEqualTo("budi@contoh.id");
    }
}
