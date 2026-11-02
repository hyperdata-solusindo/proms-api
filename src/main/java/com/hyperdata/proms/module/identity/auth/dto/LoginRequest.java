package com.hyperdata.proms.module.identity.auth.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;

public record LoginRequest(String email, String password) {
    public static final Validator<LoginRequest> VALIDATOR = ValidatorBuilder.<LoginRequest>of()
            .constraint(LoginRequest::email, "email", c -> c.notBlank()
                    .message("Email tidak boleh kosong")
                    .email()
                    .message("Format email tidak valid"))
            .constraint(LoginRequest::password, "password", c -> c.notBlank().message("Kata sandi tidak boleh kosong"))
            .build();
}
