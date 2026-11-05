package com.hyperdata.proms.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.response.RestResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerSentryTest {

    private Tracer tracer;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("sample-trace-id-12345");

        handler = new GlobalExceptionHandler(tracer);
    }

    @Test
    @DisplayName("Should handle ValidationException with 422 and return error envelope")
    void shouldHandleValidationException() {
        ValidationException ex = new ValidationException(Map.of("email", "Email is required"));
        ResponseEntity<RestResponse<Map<String, String>>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().data()).containsEntry("email", "Email is required");
    }

    @Test
    @DisplayName("Should handle GenericException with 500 and log internal error")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Database timeout");
        ResponseEntity<RestResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected internal server error occurred");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle 4xx BaseException without throwing or failing")
    void shouldHandle4xxBaseException() {
        BaseException ex = new ResourceNotFoundException("User not found");
        ResponseEntity<RestResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("User not found");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle 5xx BaseException with 500 internal server error")
    void shouldHandle5xxBaseException() {
        BaseException ex = new BusinessException("Storage disk full", HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<RestResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Storage disk full");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }
}
