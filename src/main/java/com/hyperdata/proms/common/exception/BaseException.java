package com.hyperdata.proms.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String code;
    private final String detail;
    private final HttpStatus httpStatus;
    private final Object current;

    protected BaseException(String message, HttpStatus httpStatus) {
        this(null, message, null, httpStatus, null);
    }

    protected BaseException(String code, String message, HttpStatus httpStatus) {
        this(code, message, null, httpStatus, null);
    }

    protected BaseException(String code, String message, String detail, HttpStatus httpStatus) {
        this(code, message, detail, httpStatus, null);
    }

    protected BaseException(String code, String message, String detail, HttpStatus httpStatus, Object current) {
        super(message);
        this.code = code;
        this.detail = detail;
        this.httpStatus = httpStatus;
        this.current = current;
    }
}
