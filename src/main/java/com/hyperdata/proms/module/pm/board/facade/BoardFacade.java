package com.hyperdata.proms.module.pm.board.facade;

import com.hyperdata.proms.module.pm.board.facade.dto.BoardPermissionDto;
import com.hyperdata.proms.module.pm.board.facade.dto.BoardSummaryDto;
import java.util.Optional;
import java.util.UUID;

public interface BoardFacade {

    boolean hasAccess(UUID boardId, UUID userId);

    Optional<BoardPermissionDto> getPermission(UUID boardId, UUID userId);

    void validateForTaskCreation(UUID boardId, UUID workstreamId, UUID columnId);

    Optional<BoardSummaryDto> findById(UUID boardId);
}
