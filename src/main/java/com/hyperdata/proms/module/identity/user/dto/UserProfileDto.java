package com.hyperdata.proms.module.identity.user.dto;

import java.util.UUID;

public record UserProfileDto(UUID id, String fullName, String email) {
}
