package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND,
            String.format("%s with identifier [%s] was not found", resourceName, id),
            HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String code, String message, String detail) {
        super(code, message, detail, HttpStatus.NOT_FOUND);
    }
}
