package com.hyperdata.proms.common.docs.annotation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotasi OpenAPI untuk mendokumentasikan header If-Match standar concurrency/optimistic locking.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Parameter(
    name = "If-Match",
    in = ParameterIn.HEADER,
    description = "Versi timestamp/ETag terakhir dari resource. "
        + "Mendukung format Epoch Milliseconds (contoh: \"1731301234000\")",
    required = true,
    example = "\"1731301234000\"",
    examples = {
        @ExampleObject(
            name = "Epoch Milliseconds",
            summary = "Format Epoch Millis (Paling Direkomendasikan)",
            value = "\"1731301234000\"")
    })
public @interface IfMatchHeader {
}
