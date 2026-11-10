package com.hyperdata.proms.common.exception;

import am.ik.yavi.core.ConstraintViolations;
import com.hyperdata.proms.common.response.FieldErrorDetail;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ValidationException extends BaseException {

    private final List<FieldErrorDetail> fieldErrors;

    public ValidationException(ConstraintViolations violations) {
        super(CommonErrorCode.VALIDATION, "Validation failed", null, HttpStatus.UNPROCESSABLE_ENTITY);
        this.fieldErrors = violations.stream()
            .map(cv -> new FieldErrorDetail(cv.name(), cv.message()))
            .toList();
    }

    public ValidationException(String message, List<FieldErrorDetail> fieldErrors) {
        super(CommonErrorCode.VALIDATION, message, null, HttpStatus.UNPROCESSABLE_ENTITY);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyList();
    }

    public ValidationException(String code, String message, String detail, List<FieldErrorDetail> fieldErrors) {
        super(code != null ? code : CommonErrorCode.VALIDATION, message, detail, HttpStatus.UNPROCESSABLE_ENTITY);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyList();
    }

    public ValidationException(Map<String, String> errors) {
        super(CommonErrorCode.VALIDATION, "Validation failed", null, HttpStatus.UNPROCESSABLE_ENTITY);
        this.fieldErrors = errors != null
            ? errors.entrySet()
                .stream()
                .map(e -> new FieldErrorDetail(e.getKey(), e.getValue()))
                .toList()
            : Collections.emptyList();
    }

    public Map<String, String> getErrors() {
        return fieldErrors.stream()
            .collect(Collectors.toMap(FieldErrorDetail::field, FieldErrorDetail::message,
                (existing, replacement) -> existing));
    }
}
