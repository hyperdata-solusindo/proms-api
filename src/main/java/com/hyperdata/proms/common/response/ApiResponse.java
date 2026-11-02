package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int statusCode, String message, T data) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Berhasil");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), message, data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data);
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}
