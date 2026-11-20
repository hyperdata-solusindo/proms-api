package com.hyperdata.proms.module.work.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Data ringkas user pada task")
public record TaskUserDto(
    @Schema(description = "UUID User", example = "0193a1f2-7c4e-7b8a-9d10-2f3e4a5b6c90")
    UUID userId,
    @Schema(description = "Nama lengkap", example = "Fariz Keren")
    String fullName,
    @Schema(description = "Inisial nama", example = "FK")
    String initials,
    @Schema(description = "URL foto profil")
    String avatarUrl) {

}
