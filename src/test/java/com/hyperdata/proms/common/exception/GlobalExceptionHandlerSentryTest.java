package com.hyperdata.proms.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.config.LocalizationConfig;
import com.hyperdata.proms.common.response.FieldErrorDetail;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.util.Messages;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

        org.springframework.context.support.ReloadableResourceBundleMessageSource messageSource = new org.springframework.context.support.ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(LocalizationConfig.resolveBasenames());
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(java.util.Locale.of("id", "ID"));
        new Messages(messageSource);

        handler = new GlobalExceptionHandler(tracer);
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with 400 and return error envelope")
    void shouldHandleHttpMessageNotReadableException() {
        org.springframework.http.converter.HttpMessageNotReadableException ex = new org.springframework.http.converter.HttpMessageNotReadableException(
            "JSON parse error: Unexpected character", (org.springframework.http.HttpInputMessage) null);
        ResponseEntity<RestResponse<Void>> response = handler.handleHttpMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().statusCode()).isEqualTo(400);
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Format data atau parameter tidak valid");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle ValidationException with 422 and return error envelope")
    void shouldHandleValidationException() {
        ValidationException ex = new ValidationException(Map.of("email", "Email is required"));
        ResponseEntity<RestResponse<Void>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().statusCode()).isEqualTo(422);
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION");
        assertThat(response.getBody().error().fields()).contains(new FieldErrorDetail("email", "Email is required"));
    }

    @Test
    @DisplayName("Should handle GenericException with 500 and log internal error")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Database timeout");
        ResponseEntity<RestResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().statusCode()).isEqualTo(500);
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().message()).isEqualTo("Terjadi kesalahan internal pada server");
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle 4xx BaseException without throwing or failing")
    void shouldHandle4xxBaseException() {
        BaseException ex = new ResourceNotFoundException("User not found");
        ResponseEntity<RestResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().statusCode()).isEqualTo(404);
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().message()).isEqualTo("User not found");
        assertThat(response.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle 5xx BaseException with 500 internal server error")
    void shouldHandle5xxBaseException() {
        BaseException ex = new BusinessException(
            "STORAGE_ERROR", "Storage disk full", "Disk quota exceeded", HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<RestResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().statusCode()).isEqualTo(500);
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().message()).isEqualTo("Storage disk full");
        assertThat(response.getBody().error().code()).isEqualTo("STORAGE_ERROR");
        assertThat(response.getBody().error().detail()).isEqualTo("Disk quota exceeded");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with 403 FORBIDDEN")
    void shouldHandleAccessDeniedException() {
        org.springframework.security.access.AccessDeniedException ex = new org.springframework.security.access.AccessDeniedException(
            "Access is denied");
        ResponseEntity<RestResponse<Void>> response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().statusCode()).isEqualTo(403);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("Should handle StaleVersionException with 409 and current payload")
    void shouldHandleStaleVersionException() {
        Map<String, Object> currentData = Map.of("id", "board-1", "version", 3);
        StaleVersionException ex = new StaleVersionException("Data sudah berubah", currentData);
        ResponseEntity<RestResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().statusCode()).isEqualTo(409);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.STALE_VERSION);
        assertThat(response.getBody().error().current()).isEqualTo(currentData);
    }

    @Test
    @DisplayName("Should handle IfMatchRequiredException with 428 IF_MATCH_REQUIRED")
    void shouldHandleIfMatchRequiredException() {
        IfMatchRequiredException ex = new IfMatchRequiredException();
        ResponseEntity<RestResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
        assertThat(response.getBody().statusCode()).isEqualTo(428);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.IF_MATCH_REQUIRED);
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException with 400 BAD_REQUEST")
    void shouldHandleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "not-a-uuid", UUID.class, "id", null, new IllegalArgumentException("Invalid UUID format"));
        ResponseEntity<RestResponse<Void>> response = handler.handleMethodArgumentTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().statusCode()).isEqualTo(400);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.BAD_REQUEST);
        assertThat(response.getBody().error().detail()).contains("Parameter 'id' harus berupa tipe data UUID");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle NoResourceFoundException with 404 RESOURCE_NOT_FOUND")
    void shouldHandleNoResourceFoundException() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "Resource not found",
            "/api/v1/unknown");
        ResponseEntity<RestResponse<Void>> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().statusCode()).isEqualTo(404);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        assertThat(response.getBody().error().detail()).contains("Endpoint [GET /api/v1/unknown] tidak ditemukan");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException with 405 METHOD_NOT_ALLOWED")
    void shouldHandleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST", List.of("GET"));
        ResponseEntity<RestResponse<Void>> response = handler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().statusCode()).isEqualTo(405);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().error().detail()).contains("HTTP Method 'POST' tidak didukung");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException with 409 DUPLICATE_RESOURCE")
    void shouldHandleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
            "could not execute statement", new RuntimeException("duplicate key value violates unique constraint"));
        ResponseEntity<RestResponse<Void>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().statusCode()).isEqualTo(409);
        assertThat(response.getBody().error().code()).isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE);
        assertThat(response.getBody().error().detail()).contains("pelanggaran batasan unik di database");
        assertThat(response.getBody().traceId()).isEqualTo("sample-trace-id-12345");
    }
}
