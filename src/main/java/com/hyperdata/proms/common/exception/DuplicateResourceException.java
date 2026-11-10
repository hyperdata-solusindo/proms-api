package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(CommonErrorCode.DUPLICATE_RESOURCE, message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String code, String message, String detail) {
        super(code, message, detail, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String code, String message, String detail, Object current) {
        super(code, message, detail, HttpStatus.CONFLICT, current);
    }
}
