package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class IfMatchRequiredException extends BaseException {

    public IfMatchRequiredException() {
        super(
            CommonErrorCode.IF_MATCH_REQUIRED,
            "Header If-Match diperlukan untuk operasi ini",
            "Sertakan header If-Match dengan versi entitas saat ini",
            HttpStatus.PRECONDITION_REQUIRED);
    }
}
