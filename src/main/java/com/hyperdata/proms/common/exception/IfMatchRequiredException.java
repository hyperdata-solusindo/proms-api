package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.util.Messages;
import org.springframework.http.HttpStatus;

public class IfMatchRequiredException extends BaseException {

    public IfMatchRequiredException() {
        super(
            CommonErrorCode.IF_MATCH_REQUIRED,
            Messages.get("error.if_match_required"),
            Messages.get("error.if_match_required.detail"),
            HttpStatus.PRECONDITION_REQUIRED);
    }
}
