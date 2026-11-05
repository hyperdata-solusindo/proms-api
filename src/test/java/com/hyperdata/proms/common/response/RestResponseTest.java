package com.hyperdata.proms.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestResponseTest {

    @Test
    @DisplayName("Given data and message, when ok() is called, then return RestResponse with status 200")
    void shouldCreateSuccessResponse() {
        RestResponse<String> response = RestResponse.ok("test-data", "Operation successful");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Operation successful");
        assertThat(response.data()).isEqualTo("test-data");
    }

    @Test
    @DisplayName("Given status, message and errors, when error() is called, then return RestResponse with proper values")
    void shouldCreateErrorResponseWithData() {
        RestResponse<Map<String, String>> response = RestResponse.error(
            HttpStatus.BAD_REQUEST, "Validation failed", Map.of("field", "required"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Validation failed");
        assertThat(response.data()).containsEntry("field", "required");
    }

    @Test
    @DisplayName("Given status, message, data and traceId, when error() is called, then return RestResponse with traceId")
    void shouldCreateErrorResponseWithTraceId() {
        RestResponse<String> response = RestResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null,
            "trace-123");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.message()).isEqualTo("Server Error");
        assertThat(response.traceId()).isEqualTo("trace-123");
    }
}
