package com.hyperdata.proms.module.work.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.module.work.board.constant.BoardStatus;
import com.hyperdata.proms.module.work.board.entity.Board;
import com.hyperdata.proms.module.work.board.entity.BoardMember;
import com.hyperdata.proms.module.work.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.work.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.work.board.repository.BoardRepository;
import com.hyperdata.proms.module.work.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.work.client.dto.BoardSummaryDto;
import com.hyperdata.proms.module.work.client.impl.BoardClientImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardClientImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private WorkstreamRepository workstreamRepository;

    @InjectMocks
    private BoardClientImpl boardClient;

    @Test
    @DisplayName("GIVEN member exists WHEN hasAccess THEN returns true")
    void givenMemberExists_whenHasAccess_thenReturnsTrue() {
        UUID boardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BoardMember member = new BoardMember();

        given(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).willReturn(Optional.of(member));

        boolean result = boardClient.hasAccess(boardId, userId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("GIVEN owner exists WHEN hasAccess THEN returns true")
    void givenOwnerExists_whenHasAccess_thenReturnsTrue() {
        UUID boardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Board board = new Board();
        board.setOwnerId(userId);

        given(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).willReturn(Optional.empty());
        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        boolean result = boardClient.hasAccess(boardId, userId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("GIVEN active board WHEN findById THEN returns BoardSummaryDto")
    void givenActiveBoard_whenFindById_thenReturnsSummary() {
        UUID boardId = UUID.randomUUID();
        Board board = new Board();
        board.setId(boardId);
        board.setKey("PRJ");
        board.setName("Project Alpha");
        board.setStatus("active");

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        Optional<BoardSummaryDto> summary = boardClient.findById(boardId);

        assertThat(summary).isPresent();
        assertThat(summary.get().key()).isEqualTo("PRJ");
        assertThat(summary.get().name()).isEqualTo("Project Alpha");
    }

    @Test
    @DisplayName("GIVEN archived board WHEN validateForTaskCreation THEN throws BusinessException")
    void givenArchivedBoard_whenValidateForTaskCreation_thenThrowsBusinessException() {
        UUID boardId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        Board board = new Board();
        board.setStatus(BoardStatus.ARCHIVED);

        given(boardRepository.findActiveById(boardId)).willReturn(Optional.of(board));

        assertThatThrownBy(() -> boardClient.validateForTaskCreation(boardId, null, columnId))
            .isInstanceOf(BusinessException.class);
    }
}
