package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.dto.CreateBoardTaskRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateBoardTaskRequestValidationTest {

    @Test
    @DisplayName("given blank title when validate then returns violation")
    void givenBlankTitle_whenValidate_returnsViolation() {
        var req = new CreateBoardTaskRequest("", UUID.randomUUID(), null, List.of(), LocalDate.now(), "medium");
        var violations = CreateBoardTaskRequest.validator.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.get(0).message()).contains("Judul task wajib diisi");
    }

    @Test
    @DisplayName("given null workstreamId when validate then returns violation")
    void givenNullWorkstreamId_whenValidate_returnsViolation() {
        var req = new CreateBoardTaskRequest("Task 1", null, null, List.of(), LocalDate.now(), "medium");
        var violations = CreateBoardTaskRequest.validator.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.get(0).message()).contains("Workstream wajib dipilih");
    }

    @Test
    @DisplayName("given valid request when validate then returns success")
    void givenValidRequest_whenValidate_returnsSuccess() {
        var req = new CreateBoardTaskRequest("Task 1", UUID.randomUUID(), null, List.of(), LocalDate.now(), "high");
        var violations = CreateBoardTaskRequest.validator.validate(req);
        assertThat(violations.isValid()).isTrue();
    }
}
