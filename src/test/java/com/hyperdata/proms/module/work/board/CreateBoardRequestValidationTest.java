package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.dto.CreateBoardRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateBoardRequestValidationTest {

    @Test
    @DisplayName("Request valid harus lolos validasi YAVI")
    void validRequestShouldPass() {
        var req = new CreateBoardRequest(
            "Nusa Bioscience IoT",
            "NBI",
            "PT Nusa",
            "IoT",
            UUID.randomUUID(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            null,
            50_000_000L);
        var violations = CreateBoardRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    @DisplayName("Key yang tidak memenuhi format 2-5 huruf kapital harus gagal validasi")
    void invalidKeyShouldFail() {
        var req = new CreateBoardRequest(
            "Nusa",
            "lowercase",
            null, null, null, null, null, null, null);
        var violations = CreateBoardRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("key"));
    }

    @Test
    @DisplayName("Rentang tanggal tidak valid (startDate > dueDate) harus gagal validasi")
    void invalidDateRangeShouldFail() {
        var req = new CreateBoardRequest(
            "Nusa",
            "NUSA",
            null, null, null,
            LocalDate.now().plusDays(10),
            LocalDate.now(),
            null, null);
        var violations = CreateBoardRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("dueDate"));
    }
}
