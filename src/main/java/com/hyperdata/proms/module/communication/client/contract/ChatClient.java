package com.hyperdata.proms.module.communication.client.contract;

import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import java.util.List;
import java.util.UUID;

public interface ChatClient {

    List<ChatChannelSummaryDto> listChannelsByBoardId(UUID tenantId, UUID boardId);
}
