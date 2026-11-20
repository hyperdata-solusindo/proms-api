package com.hyperdata.proms.module.work.client.contract;

import com.hyperdata.proms.module.work.client.dto.BoardPermissionDto;
import com.hyperdata.proms.module.work.client.dto.BoardSummaryDto;
import java.util.Optional;
import java.util.UUID;

public interface BoardClient {

    boolean hasAccess(UUID boardId, UUID userId);

    Optional<BoardPermissionDto> getPermission(UUID boardId, UUID userId);

    void validateForTaskCreation(UUID boardId, UUID workstreamId, UUID columnId);

    Optional<BoardSummaryDto> findById(UUID boardId);
}
