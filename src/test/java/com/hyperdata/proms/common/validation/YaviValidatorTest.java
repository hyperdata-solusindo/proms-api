package com.hyperdata.proms.common.validation;

import static org.junit.jupiter.api.Assertions.*;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import com.hyperdata.proms.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class YaviValidatorTest {

    record SampleRequest(String name) {}

    static final Validator<SampleRequest> validator = ValidatorBuilder.<SampleRequest>of()
            .constraint(SampleRequest::name, "name", c -> c.notBlank().message("Name is mandatory"))
            .build();

    @Test
    @DisplayName("Given invalid request, when validateAndThrow is called, then throw" + " ValidationException")
    void shouldThrowWhenInvalid() {
        SampleRequest invalid = new SampleRequest("");

        ValidationException ex =
                assertThrows(ValidationException.class, () -> YaviValidator.validateAndThrow(validator, invalid));

        assertTrue(ex.getErrors().containsKey("name"));
        assertEquals("Name is mandatory", ex.getErrors().get("name"));
    }

    @Test
    @DisplayName("Given valid request, when validateAndThrow is called, then pass without exception")
    void shouldPassWhenValid() {
        SampleRequest valid = new SampleRequest("Valid Name");
        assertDoesNotThrow(() -> YaviValidator.validateAndThrow(validator, valid));
    }
}
