package com.hyperdata.proms.common.exception;

import org.springframework.http.HttpStatus;

public class StaleVersionException extends BaseException {

    public StaleVersionException(String message, Object current) {
        super(CommonErrorCode.STALE_VERSION, message, "Data sudah berubah, dimuat ulang", HttpStatus.CONFLICT, current);
    }

    public StaleVersionException(Object current) {
        this("Data telah diperbarui oleh pengguna lain", current);
    }
}
