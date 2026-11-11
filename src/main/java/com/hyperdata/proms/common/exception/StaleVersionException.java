package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.util.Messages;
import org.springframework.http.HttpStatus;

public class StaleVersionException extends BaseException {

    public StaleVersionException(String message, Object current) {
        super(
            CommonErrorCode.STALE_VERSION,
            message,
            Messages.get("error.stale_version.detail"),
            HttpStatus.CONFLICT,
            current);
    }

    public StaleVersionException(Object current) {
        this(Messages.get("error.stale_version"), current);
    }
}
