package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.response.RestResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Tracer tracer;

    public GlobalExceptionHandler() {
        this(null);
    }

    @Autowired
    public GlobalExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<RestResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
        captureToSentry(ex, SentryLevel.WARNING, "validation");
        RestResponse<Map<String, String>> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<RestResponse<Void>> handleBaseException(BaseException ex) {
        SentryLevel level = ex.getHttpStatus().is4xxClientError() ? SentryLevel.WARNING : SentryLevel.ERROR;
        captureToSentry(ex, level, ex.getClass().getSimpleName());

        RestResponse<Void> response = RestResponse.error(ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        captureToSentry(ex, SentryLevel.ERROR, "unhandled_internal");

        RestResponse<Void> response = RestResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private void captureToSentry(Throwable ex, SentryLevel level, String category) {
        Sentry.withScope(scope -> {
            scope.setLevel(level);
            scope.setTag("error.category", category);
            if (tracer != null) {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null && currentSpan.context() != null) {
                    scope.setTag("otel.trace_id", currentSpan.context().traceId());
                }
            }
            Sentry.captureException(ex);
        });
    }
}
