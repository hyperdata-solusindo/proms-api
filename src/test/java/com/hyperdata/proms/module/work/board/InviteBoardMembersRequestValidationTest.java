package com.hyperdata.proms.module.work.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.dto.InviteBoardMembersRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InviteBoardMembersRequestValidationTest {

    @Test
    @DisplayName("Request invite valid harus lolos validasi YAVI")
    void validRequestShouldPass() {
        var req = new InviteBoardMembersRequest(
            List.of("budi@contoh.id", "siti@contoh.id"),
            BoardMemberRole.MEMBER,
            List.of());
        var violations = InviteBoardMembersRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    @DisplayName("Request invite dengan emails kosong harus gagal validasi")
    void emptyEmailsShouldFail() {
        var req = new InviteBoardMembersRequest(
            List.of(),
            BoardMemberRole.MEMBER,
            List.of());
        var violations = InviteBoardMembersRequest.VALIDATOR.validate(req);
        assertThat(violations.isValid()).isFalse();
        assertThat(violations.violations()).anyMatch(v -> v.name().equals("emails"));
    }
}
