package com.hyperdata.proms.module.identity.auth.dto;

import java.util.UUID;

public record AuthUserDto(UUID id, String fullName, String email, String locale, String timezone) {}
