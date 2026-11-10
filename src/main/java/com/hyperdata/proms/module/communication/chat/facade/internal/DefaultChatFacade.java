package com.hyperdata.proms.module.communication.chat.facade.internal;

import com.hyperdata.proms.module.communication.chat.facade.ChatFacade;
import com.hyperdata.proms.module.communication.chat.facade.dto.ChatChannelSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultChatFacade implements ChatFacade {

    @Override
    public List<ChatChannelSummaryDto> listChannelsByBoardId(UUID tenantId, UUID boardId) {
        return List.of(
            new ChatChannelSummaryDto(
                UUID.randomUUID(),
                "board",
                boardId,
                "general",
                0,
                new ChatChannelSummaryDto.LastMessageDto(
                    "Welcome to board chat channel",
                    Instant.now(),
                    new ChatChannelSummaryDto.AuthorRefDto(UUID.randomUUID(), "System")),
                false,
                false));
    }
}
