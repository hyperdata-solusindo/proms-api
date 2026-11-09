package com.hyperdata.proms.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Given data and message, when ok() is called, then return RestResponse with status 200")
    void shouldCreateSuccessResponse() {
        RestResponse<String> response = RestResponse.ok("test-data", "Operation successful");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Operation successful");
        assertThat(response.data()).isEqualTo("test-data");
        assertThat(response.meta()).isNull();
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("Given data, meta, and message, when ok() is called, then return RestResponse with meta")
    void shouldCreateSuccessResponseWithMeta() {
        Map<String, Object> meta = Map.of("nextCursor", "cursor-123", "unreadCount", 5);
        RestResponse<List<String>> response = RestResponse.ok(List.of("item1", "item2"), meta, "List retrieved");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("List retrieved");
        assertThat(response.data()).containsExactly("item1", "item2");
        assertThat(response.meta()).isEqualTo(meta);
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("Given error details, when error() is called, then return RestResponse with ErrorDetail")
    void shouldCreateErrorResponseWithErrorDetail() {
        FieldErrorDetail fieldError = new FieldErrorDetail("blockedReason", "Wajib diisi");
        ErrorDetail errorDetail = new ErrorDetail(
            "BR-WF-09", "Isi alasan Blocked lalu coba lagi.", List.of(fieldError));

        RestResponse<Void> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, "Alasan wajib diisi saat memindah task ke Blocked", errorDetail);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.message()).isEqualTo("Alasan wajib diisi saat memindah task ke Blocked");
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo("BR-WF-09");
        assertThat(response.error().detail()).isEqualTo("Isi alasan Blocked lalu coba lagi.");
        assertThat(response.error().fields()).containsExactly(fieldError);
    }

    @Test
    @DisplayName("Given error response, when serialized to JSON, then include data: null and error object without meta")
    void shouldSerializeErrorResponseToJsonWithExplicitNullData() throws Exception {
        FieldErrorDetail fieldError = new FieldErrorDetail("blockedReason", "Wajib diisi");
        ErrorDetail errorDetail = new ErrorDetail(
            "BR-WF-09", "Isi alasan Blocked lalu coba lagi.", List.of(fieldError));

        RestResponse<Void> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, "Alasan wajib diisi saat memindah task ke Blocked", errorDetail);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"statusCode\":422");
        assertThat(json).contains("\"message\":\"Alasan wajib diisi saat memindah task ke Blocked\"");
        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"error\":{");
        assertThat(json).contains("\"code\":\"BR-WF-09\"");
        assertThat(json).contains("\"detail\":\"Isi alasan Blocked lalu coba lagi.\"");
        assertThat(json).contains("\"fields\":[{\"field\":\"blockedReason\",\"message\":\"Wajib diisi\"}]");
        assertThat(json).doesNotContain("\"meta\"");
        assertThat(json).doesNotContain("\"traceId\"");
    }

    @Test
    @DisplayName("Given status, message, error, and traceId, when error() is called, then return RestResponse with traceId")
    void shouldCreateErrorResponseWithTraceId() {
        ErrorDetail errorDetail = new ErrorDetail("INTERNAL_SERVER_ERROR", "Server crashed");
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", errorDetail, "trace-123");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.message()).isEqualTo("Server Error");
        assertThat(response.error()).isEqualTo(errorDetail);
        assertThat(response.traceId()).isEqualTo("trace-123");
    }
}
