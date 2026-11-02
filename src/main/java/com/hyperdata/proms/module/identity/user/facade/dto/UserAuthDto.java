package com.hyperdata.proms.module.identity.user.facade.dto;

import java.util.UUID;

public record UserAuthDto(
        UUID id, String email, String password, String fullName, String locale, String timezone, boolean isActive) {}
