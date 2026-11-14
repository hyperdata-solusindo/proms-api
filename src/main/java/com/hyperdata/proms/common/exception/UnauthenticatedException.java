package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.util.Messages;
import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends BaseException {

    public UnauthenticatedException() {
        this(Messages.get("error.unauthorized"));
    }

    public UnauthenticatedException(String message) {
        super(
            CommonErrorCode.UNAUTHENTICATED,
            message,
            Messages.get("error.unauthorized.detail"),
            HttpStatus.UNAUTHORIZED);
    }
}
