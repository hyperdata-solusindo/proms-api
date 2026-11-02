package com.hyperdata.proms.common.validation;

import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import com.hyperdata.proms.common.exception.ValidationException;

public final class YaviValidator {

    private YaviValidator() {}

    public static <T> void validateAndThrow(Validator<T> validator, T target) {
        ConstraintViolations violations = validator.validate(target);
        if (!violations.isValid()) {
            throw new ValidationException(violations);
        }
    }
}
