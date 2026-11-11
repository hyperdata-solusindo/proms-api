package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends BaseException {

    public UnauthenticatedException() {
        super(
            CommonErrorCode.UNAUTHENTICATED,
            "Autentikasi diperlukan untuk mengakses endpoint ini",
            "Token autentikasi tidak valid atau belum disertakan",
            HttpStatus.UNAUTHORIZED);
    }

    public UnauthenticatedException(String message) {
        super(
            CommonErrorCode.UNAUTHENTICATED,
            message,
            "Token autentikasi tidak valid atau belum disertakan",
            HttpStatus.UNAUTHORIZED);
    }
}
