package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardViewType;
import com.hyperdata.proms.module.work.board.dto.CreateSavedViewRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateSavedViewRequestValidationTest {

    @Test
    @DisplayName("Request valid harus lolos validasi YAVI")
    void validRequestShouldPass() {
        var req = new CreateSavedViewRequest(
            "Hardware Overdue",
            BoardViewType.LIST,
            Map.of("overdue", true),
            Map.of(),
            null,
            false);
        var violations = CreateSavedViewRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    @DisplayName("Nama view yang kosong harus gagal validasi")
    void emptyNameShouldFail() {
        var req = new CreateSavedViewRequest(
            "",
            BoardViewType.LIST,
            Map.of(),
            Map.of(),
            null,
            false);
        var violations = CreateSavedViewRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("name"));
    }
}
