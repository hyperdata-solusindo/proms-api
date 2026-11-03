package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public record ApiResponse<T>(
    @Schema(description = "Kode status HTTP", example = "200")
    int statusCode,
    @Schema(description = "Pesan keterangan status respon", example = "Berhasil")
    String message,
    @Schema(description = "Data payload respon")
    T data) {

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
