package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.util.Messages;
import org.springframework.http.HttpStatus;

/**
 * Exception yang dilempar saat operasi membutuhkan konteks tenant aktif (Fail-Closed).
 */
public class TenantRequiredException extends BaseException {

    public TenantRequiredException() {
        this(Messages.get("error.forbidden.tenant_required"));
    }

    public TenantRequiredException(String message) {
        super(
            CommonErrorCode.FORBIDDEN,
            message,
            Messages.get("error.forbidden.tenant_required.detail"),
            HttpStatus.FORBIDDEN);
    }
}
