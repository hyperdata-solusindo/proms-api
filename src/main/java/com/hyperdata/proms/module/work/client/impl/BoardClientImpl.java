package com.hyperdata.proms.module.work.client.impl;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.constant.BoardStatus;
import com.hyperdata.proms.module.work.board.entity.Board;
import com.hyperdata.proms.module.work.board.entity.BoardColumn;
import com.hyperdata.proms.module.work.board.entity.BoardMember;
import com.hyperdata.proms.module.work.board.repository.BoardColumnRepository;
import com.hyperdata.proms.module.work.board.repository.BoardMemberRepository;
import com.hyperdata.proms.module.work.board.repository.BoardRepository;
import com.hyperdata.proms.module.work.board.repository.WorkstreamRepository;
import com.hyperdata.proms.module.work.client.contract.BoardClient;
import com.hyperdata.proms.module.work.client.dto.BoardPermissionDto;
import com.hyperdata.proms.module.work.client.dto.BoardSummaryDto;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardClientImpl implements BoardClient {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final WorkstreamRepository workstreamRepository;

    @Override
    public boolean hasAccess(UUID boardId, UUID userId) {
        return boardMemberRepository.findByBoardIdAndUserId(boardId, userId).isPresent() ||
            boardRepository.findActiveById(boardId).map(b -> b.getOwnerId().equals(userId)).orElse(false);
    }

    @Override
    public Optional<BoardPermissionDto> getPermission(UUID boardId, UUID userId) {
        Optional<Board> boardOpt = boardRepository.findActiveById(boardId);
        if (boardOpt.isEmpty()) {
            return Optional.empty();
        }
        Board board = boardOpt.get();
        boolean isOwner = board.getOwnerId().equals(userId);
        Optional<BoardMember> memberOpt = boardMemberRepository.findByBoardIdAndUserId(boardId, userId);
        if (!isOwner && memberOpt.isEmpty()) {
            return Optional.empty();
        }

        String role = memberOpt.map(BoardMember::getRole)
            .orElse(isOwner ? BoardMemberRole.OWNER : BoardMemberRole.VIEWER);
        boolean memberIsOwner = isOwner || memberOpt.map(BoardMember::getIsOwner).orElse(false);

        return Optional.of(new BoardPermissionDto(
            boardId,
            userId,
            role,
            memberIsOwner,
            board.getStatus()));
    }

    @Override
    public void validateForTaskCreation(UUID boardId, UUID workstreamId, UUID columnId) {
        Board board = boardRepository.findActiveById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));

        if (BoardStatus.ARCHIVED.equalsIgnoreCase(board.getStatus())) {
            throw new BusinessException(
                CommonErrorCode.BOARD_ARCHIVED,
                Messages.get("board.error.archived"),
                HttpStatus.CONFLICT);
        }

        boardColumnRepository.findActiveColumnById(columnId, boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", columnId));

        if (workstreamId != null) {
            workstreamRepository.findActiveWorkstreamById(workstreamId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Workstream", workstreamId));
        }
    }

    @Override
    public Optional<BoardSummaryDto> findById(UUID boardId) {
        return boardRepository.findActiveById(boardId)
            .map(b -> new BoardSummaryDto(b.getId(), b.getKey(), b.getName(), b.getStatus()));
    }

    @Override
    public Map<UUID, String> getColumnCategories(Collection<UUID> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) {
            return Map.of();
        }
        return Lookups.toMap(
            boardColumnRepository.findActiveColumnsByBoardIds(boardIds),
            BoardColumn::getId,
            BoardColumn::getCategory);
    }
}
