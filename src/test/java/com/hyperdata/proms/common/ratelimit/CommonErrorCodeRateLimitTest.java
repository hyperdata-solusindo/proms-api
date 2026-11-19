package com.hyperdata.proms.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommonErrorCodeRateLimitTest {

    @Test
    @DisplayName("Should define RATE_LIMIT_EXCEEDED constant in CommonErrorCode")
    void shouldDefineRateLimitExceededConstant() {
        assertThat(CommonErrorCode.RATE_LIMIT_EXCEEDED).isEqualTo("RATE_LIMIT_EXCEEDED");
    }
}
