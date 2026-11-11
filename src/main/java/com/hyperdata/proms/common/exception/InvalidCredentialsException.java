package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.util.Messages;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseException {

    public InvalidCredentialsException() {
        this(Messages.get("auth.error.invalid_credentials"));
    }

    public InvalidCredentialsException(String message) {
        super(
            CommonErrorCode.UNAUTHENTICATED,
            message,
            Messages.get("auth.error.invalid_credentials.detail"),
            HttpStatus.UNAUTHORIZED);
    }
}
