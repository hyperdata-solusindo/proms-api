package com.hyperdata.proms.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detail kesalahan per field untuk validasi")
public record FieldErrorDetail(
    @Schema(description = "Nama field yang mengalami kesalahan validasi", example = "blockedReason")
    String field,
    @Schema(description = "Pesan keterangan kesalahan validasi", example = "Wajib diisi")
    String message) {

    public static FieldErrorDetail of(String field, String message) {
        return new FieldErrorDetail(field, message);
    }
}
