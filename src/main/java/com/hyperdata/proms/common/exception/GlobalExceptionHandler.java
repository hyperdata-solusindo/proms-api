package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.response.ErrorDetail;
import com.hyperdata.proms.common.response.FieldErrorDetail;
import com.hyperdata.proms.common.response.RestResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectProvider<Tracer> tracerProvider;

    public GlobalExceptionHandler() {
        this((ObjectProvider<Tracer>) null);
    }

    public GlobalExceptionHandler(@Nullable
    Tracer tracer) {
        this(new ObjectProvider<>() {

            @Override
            public Tracer getObject(Object... args) {
                return tracer;
            }

            @Override
            public Tracer getIfAvailable() {
                return tracer;
            }

            @Override
            public Tracer getIfUnique() {
                return tracer;
            }

            @Override
            public Tracer getObject() {
                return tracer;
            }
        });
    }

    public GlobalExceptionHandler(@Nullable
    ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<RestResponse<Void>> handleHttpMessageNotReadable(
        org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed HTTP request body: {}", ex.getMessage());
        String traceId = resolveCurrentTraceId();
        ErrorDetail errorDetail = new ErrorDetail(
            CommonErrorCode.BAD_REQUEST, "Format payload request JSON tidak valid atau rusak", null, null);
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.BAD_REQUEST, "Format data tidak valid", errorDetail, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<RestResponse<Void>> handleValidationException(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        String traceId = resolveCurrentTraceId();
        String code = ex.getCode() != null ? ex.getCode() : CommonErrorCode.VALIDATION;
        ErrorDetail errorDetail = new ErrorDetail(code, ex.getDetail(), ex.getFieldErrors(), null);
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), errorDetail, traceId);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
            .toList();
        String traceId = resolveCurrentTraceId();
        ErrorDetail errorDetail = new ErrorDetail(CommonErrorCode.VALIDATION, "Format request atau field tidak valid",
            fieldErrors,
            null);
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, "Format request tidak valid", errorDetail, traceId);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<RestResponse<Void>> handleAccessDeniedException(
        org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        String traceId = resolveCurrentTraceId();
        ErrorDetail errorDetail = new ErrorDetail(
            CommonErrorCode.FORBIDDEN, "Anda tidak memiliki izin untuk mengakses resource ini", null, null);
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.FORBIDDEN, "Akses ditolak", errorDetail, traceId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<RestResponse<Void>> handleBaseException(BaseException ex) {
        String traceId = resolveCurrentTraceId();
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Internal service error: ", ex);
            captureToSentry(ex, SentryLevel.ERROR, ex.getClass().getSimpleName());
        } else {
            log.warn("Client exception [{}]: {}", ex.getHttpStatus(), ex.getMessage());
        }

        String code = ex.getCode() != null ? ex.getCode() : ex.getHttpStatus().name();
        ErrorDetail errorDetail = new ErrorDetail(code, ex.getDetail(), null, ex.getCurrent());
        RestResponse<Void> response = RestResponse.error(ex.getHttpStatus(), ex.getMessage(), errorDetail, traceId);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        captureToSentry(ex, SentryLevel.ERROR, "unhandled_internal");

        String traceId = resolveCurrentTraceId();
        ErrorDetail errorDetail = new ErrorDetail(
            CommonErrorCode.INTERNAL_SERVER_ERROR, "Terjadi kesalahan internal pada server", null, null);
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred", errorDetail, traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String resolveCurrentTraceId() {
        Tracer tracer = tracerProvider != null ? tracerProvider.getIfAvailable() : null;
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                String traceId = currentSpan.context().traceId();
                if (traceId != null && !traceId.isBlank()) {
                    return traceId;
                }
            }
        }
        String mdcTraceId = MDC.get("traceId");
        if (mdcTraceId != null && !mdcTraceId.isBlank()) {
            return mdcTraceId;
        }
        return null;
    }

    private void captureToSentry(Throwable ex, SentryLevel level, String category) {
        Sentry.withScope(scope -> {
            scope.setLevel(level);
            scope.setTag("error.category", category);

            Tracer tracer = tracerProvider != null ? tracerProvider.getIfAvailable() : null;
            if (tracer != null) {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null && currentSpan.context() != null) {
                    String traceId = currentSpan.context().traceId();
                    if (traceId != null && !traceId.isBlank()) {
                        scope.setTag("trace_id", traceId);
                        scope.setTag("traceId", traceId);
                        scope.setTag("otel.trace_id", traceId);
                        try {
                            scope.getPropagationContext().setTraceId(new io.sentry.protocol.SentryId(traceId));
                        } catch (Exception ignored) {
                            // Fallback gracefully if traceId format differs
                        }
                    }
                }
            }

            Sentry.captureException(ex);
        });
    }
}
