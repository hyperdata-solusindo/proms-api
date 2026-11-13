package com.hyperdata.proms.module.pm.client.contract;

import com.hyperdata.proms.module.pm.client.dto.BoardPermissionDto;
import com.hyperdata.proms.module.pm.client.dto.BoardSummaryDto;
import java.util.Optional;
import java.util.UUID;

public interface BoardClient {

    boolean hasAccess(UUID boardId, UUID userId);

    Optional<BoardPermissionDto> getPermission(UUID boardId, UUID userId);

    void validateForTaskCreation(UUID boardId, UUID workstreamId, UUID columnId);

    Optional<BoardSummaryDto> findById(UUID boardId);
}
