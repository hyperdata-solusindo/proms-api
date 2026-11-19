package com.hyperdata.proms.module.identity.auth.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload permintaan login autentikasi pengguna")
public record LoginRequest(
    @Schema(
        description = "Alamat email akun terdaftar",
        example = "admin@hyperdata.id",
        format = "email",
        requiredMode = REQUIRED)
    String email,
    @Schema(
        description = "Kata sandi akun pengguna",
        example = "password123",
        format = "password",
        requiredMode = REQUIRED)
    String password) {

    public static final Validator<LoginRequest> validator = ValidatorBuilder.<LoginRequest>of()
        .constraint(LoginRequest::email, "email", c -> c.notBlank()
            .message("Email tidak boleh kosong")
            .email()
            .message("Format email tidak valid"))
        .constraint(LoginRequest::password, "password", c -> c.notBlank()
            .message("Kata sandi tidak boleh kosong"))
        .build();
}
