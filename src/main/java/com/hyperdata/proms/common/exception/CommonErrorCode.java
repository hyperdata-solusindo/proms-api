package com.hyperdata.proms.common.exception;

/**
 * Standard error codes contract required to be handled by client applications.
 */
public final class CommonErrorCode {

    private CommonErrorCode() {
    }

    // 400
    public static final String BAD_REQUEST = "BAD_REQUEST";

    // 401
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    // 403
    public static final String TENANT_ACCESS_REVOKED = "TENANT_ACCESS_REVOKED";
    public static final String TENANT_READ_ONLY = "TENANT_READ_ONLY";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String BR_WF_06 = "BR-WF-06";
    public static final String BR_WF_07 = "BR-WF-07";

    // 404
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String BOARD_NOT_FOUND = "BOARD_NOT_FOUND";
    public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";

    // 405
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";

    // 409
    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";
    public static final String STALE_VERSION = "STALE_VERSION";
    public static final String BOARD_ARCHIVED = "BOARD_ARCHIVED";
    public static final String BR_WF_08 = "BR-WF-08";

    // 422
    public static final String VALIDATION = "VALIDATION";
    public static final String BR_WF_09 = "BR-WF-09";

    // 428
    public static final String IF_MATCH_REQUIRED = "IF_MATCH_REQUIRED";

    // 500
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
}
