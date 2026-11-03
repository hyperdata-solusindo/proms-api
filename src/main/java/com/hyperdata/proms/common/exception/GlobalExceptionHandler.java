package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.response.RestResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<RestResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
        RestResponse<Map<String, String>> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<RestResponse<Void>> handleBaseException(BaseException ex) {
        RestResponse<Void> response = RestResponse.error(ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        RestResponse<Void> response = RestResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
