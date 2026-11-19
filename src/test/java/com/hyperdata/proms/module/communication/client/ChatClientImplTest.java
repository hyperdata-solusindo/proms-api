package com.hyperdata.proms.module.communication.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.communication.client.dto.ChatChannelSummaryDto;
import com.hyperdata.proms.module.communication.client.impl.ChatClientImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatClientImplTest {

    private final ChatClientImpl chatClient = new ChatClientImpl();

    @Test
    @DisplayName("GIVEN tenantId and boardId WHEN listChannelsByBoardId THEN returns stub chat channel")
    void givenTenantAndBoard_whenListChannels_thenReturnsChannel() {
        UUID tenantId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();

        List<ChatChannelSummaryDto> channels = chatClient.listChannelsByBoardId(tenantId, boardId);

        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).boardId()).isEqualTo(boardId);
        assertThat(channels.get(0).name()).isEqualTo("general");
    }
}
