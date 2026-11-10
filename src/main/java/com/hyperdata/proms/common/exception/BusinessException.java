package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String code, String message, String detail) {
        super(code, message, detail, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String message, HttpStatus httpStatus) {
        super("BUSINESS_ERROR", message, httpStatus);
    }

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(code, message, httpStatus);
    }

    public BusinessException(String code, String message, String detail, HttpStatus httpStatus) {
        super(code, message, detail, httpStatus);
    }

    public BusinessException(String code, String message, String detail, HttpStatus httpStatus, Object current) {
        super(code, message, detail, httpStatus, current);
    }
}
