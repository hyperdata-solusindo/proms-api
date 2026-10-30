package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
