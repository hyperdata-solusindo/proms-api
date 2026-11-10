package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseException {

    public InvalidCredentialsException() {
        super(
            CommonErrorCode.UNAUTHENTICATED,
            "Email atau kata sandi tidak sesuai",
            "Pastikan email dan kata sandi yang Anda masukkan sudah benar.",
            HttpStatus.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message) {
        super(
            CommonErrorCode.UNAUTHENTICATED,
            message,
            "Pastikan email dan kata sandi yang Anda masukkan sudah benar.",
            HttpStatus.UNAUTHORIZED);
    }
}
