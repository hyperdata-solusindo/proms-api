package com.hyperdata.proms.common.exception;

import am.ik.yavi.core.ConstraintViolations;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ValidationException extends BaseException {
    private final Map<String, String> errors;

    public ValidationException(ConstraintViolations violations) {
        super("Validation failed", HttpStatus.BAD_REQUEST);
        this.errors =
                violations.stream()
                        .collect(
                                Collectors.toMap(
                                        cv -> cv.name(),
                                        cv -> cv.message(),
                                        (existing, replacement) -> existing));
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed", HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }
}
