package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "Standard REST API response envelope")
public record RestResponse<T>(
    @Schema(description = "Kode status HTTP", example = "200")
    int statusCode,
    @Schema(description = "Pesan keterangan status respon", example = "Berhasil")
    String message,
    @Schema(description = "Data payload respon")
    T data,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Metadata tambahan untuk daftar (nextCursor, summary, dll)")
    Object meta,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Detail informasi error jika request gagal")
    ErrorDetail error,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
        description = "Trace ID unik untuk keperluan debugging dan pelaporan error",
        example = "4bf92f3577b34da6a3ce929d0e0e4736")
    String traceId) {

    public RestResponse(int statusCode, String message, T data) {
        this(statusCode, message, data, null, null, null);
    }

    public RestResponse(int statusCode, String message, T data, String traceId) {
        this(statusCode, message, data, null, null, traceId);
    }

    public RestResponse(int statusCode, String message, T data, ErrorDetail error, String traceId) {
        this(statusCode, message, data, null, error, traceId);
    }

    public static <T> RestResponse<T> ok(T data, String message) {
        return new RestResponse<>(HttpStatus.OK.value(), message, data, null, null, null);
    }

    public static <T> RestResponse<T> ok(T data) {
        return ok(data, "Berhasil");
    }

    public static <T> RestResponse<T> ok(T data, Object meta, String message) {
        return new RestResponse<>(HttpStatus.OK.value(), message, data, meta, null, null);
    }

    public static <T> RestResponse<T> created(T data, String message) {
        return new RestResponse<>(HttpStatus.CREATED.value(), message, data, null, null, null);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, ErrorDetail error) {
        return new RestResponse<>(status.value(), message, null, null, error, null);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, ErrorDetail error, String traceId) {
        return new RestResponse<>(status.value(), message, null, null, error, traceId);
    }

    public static RestResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, (ErrorDetail) null, (String) null);
    }

    public static RestResponse<Void> error(HttpStatus status, String message, String traceId) {
        return error(status, message, (ErrorDetail) null, traceId);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, T data) {
        return new RestResponse<>(status.value(), message, data, null, null, null);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, T data, String traceId) {
        return new RestResponse<>(status.value(), message, data, null, null, traceId);
    }
}
