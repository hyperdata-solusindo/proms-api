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
    T data) {

    public static <T> RestResponse<T> ok(T data, String message) {
        return new RestResponse<>(HttpStatus.OK.value(), message, data);
    }

    public static <T> RestResponse<T> ok(T data) {
        return ok(data, "Berhasil");
    }

    public static <T> RestResponse<T> created(T data, String message) {
        return new RestResponse<>(HttpStatus.CREATED.value(), message, data);
    }

    public static <T> RestResponse<T> error(HttpStatus status, String message, T data) {
        return new RestResponse<>(status.value(), message, data);
    }

    public static RestResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}
