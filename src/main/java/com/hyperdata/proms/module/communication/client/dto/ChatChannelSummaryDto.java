package com.hyperdata.proms.module.communication.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatChannelSummaryDto(
    UUID id,
    String type,
    UUID boardId,
    String name,
    int unreadCount,
    LastMessageDto lastMessage,
    boolean isStarred,
    boolean isMuted) {

    public record LastMessageDto(
        String text,
        Instant createdAt,
        AuthorRefDto author) {
    }

    public record AuthorRefDto(UUID id, String fullName) {
    }
}
