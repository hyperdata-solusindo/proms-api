package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception yang dilempar saat operasi membutuhkan konteks tenant aktif (Fail-Closed).
 */
public class TenantRequiredException extends BaseException {

    public TenantRequiredException() {
        this("Operasi ini memerlukan konteks tenant aktif yang valid.");
    }

    public TenantRequiredException(String message) {
        super(CommonErrorCode.FORBIDDEN, message, "activeTenantId bernilai null atau belum dipilih",
            HttpStatus.FORBIDDEN);
    }
}
