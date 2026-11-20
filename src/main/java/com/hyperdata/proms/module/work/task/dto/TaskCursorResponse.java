package com.hyperdata.proms.module.work.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Satu halaman data dengan cursor halaman berikutnya")
public record TaskCursorResponse<T>(
    @Schema(description = "Item pada halaman ini")
    List<T> items,
    @Schema(description = "Cursor halaman berikutnya; null bila sudah halaman terakhir", example = "eyJvIjo1MH0")
    String nextCursor) {

    @Schema(description = "Metadata paging cursor")
    public record Meta(
        @Schema(
            description = "Cursor halaman berikutnya; null bila halaman sudah halaman terakhir",
            example = "eyJvIjo1MH0")
        String nextCursor) {
    }
    public Meta meta() {
        return new Meta(nextCursor);
    }

}
