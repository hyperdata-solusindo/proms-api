package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard REST API response envelope")
public record RestResponse<T>(
    @Schema(description = "Kode status HTTP", example = "200")
    int statusCode,
    @Schema(description = "Pesan keterangan status respon", example = "Berhasil")
    String message,
    @Schema(description = "Data payload respon")
    T data,
    @Schema(
        description = "Trace ID unik untuk keperluan debugging dan pelaporan error",
        example = "4bf92f3577b34da6a3ce929d0e0e4736")
    String traceId) {

    public RestResponse(int statusCode, String message, T data) {
        this(statusCode, message, data, null);
    }

    public static <T> RestResponse<T> ok(T data, String message) {
        return new RestResponse<>(HttpStatus.OK.value(), message, data, null);
    }

    public static <T> RestResponse<T> ok(T data) {
        return ok(data, "Berhasil");
    }

    public static <T> RestResponse<T> created(T data, String message) {
        return new RestResponse<>(HttpStatus.CREATED.value(), message, data, null);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, T data) {
        return new RestResponse<>(status.value(), message, data, null);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, T data, String traceId) {
        return new RestResponse<>(status.value(), message, data, traceId);
    }

    public static RestResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null, null);
    }

    public static RestResponse<Void> error(HttpStatus status, String message, String traceId) {
        return error(status, message, null, traceId);
    }
}
