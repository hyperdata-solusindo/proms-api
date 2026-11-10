package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detail informasi error standar")
public record ErrorDetail(
    @Schema(description = "Kode BR atau kode teknis", example = "VALIDATION")
    String code,
    @Schema(description = "Penjelasan tambahan (opsional)", example = "Periksa kembali field yang diisi.")
    String detail,
    @Schema(description = "Daftar kesalahan per field untuk 422 VALIDATION (opsional)")
    List<FieldErrorDetail> fields,
    @Schema(description = "Data terbaru untuk 409 STALE_VERSION (opsional)")
    Object current) {

    public ErrorDetail(String code) {
        this(code, null, null, null);
    }

    public ErrorDetail(String code, String detail) {
        this(code, detail, null, null);
    }

    public ErrorDetail(String code, String detail, List<FieldErrorDetail> fields) {
        this(code, detail, fields, null);
    }
}
