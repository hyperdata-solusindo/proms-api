package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success, int status, String message, T data, Map<String, String> errors, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Success");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), message, data, null, Instant.now());
    }

    public static ApiResponse<Void> error(HttpStatus status, String message, Map<String, String> errors) {
        return new ApiResponse<>(false, status.value(), message, null, errors, Instant.now());
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}
