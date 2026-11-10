package com.hyperdata.proms.module.communication.chat.facade;

import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import java.util.List;
import java.util.UUID;

public interface ChatFacade {

    List<ChatChannelSummaryDto> listChannelsByBoardId(UUID tenantId, UUID boardId);
}
