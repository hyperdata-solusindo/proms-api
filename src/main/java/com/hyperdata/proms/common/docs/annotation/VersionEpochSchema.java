package com.hyperdata.proms.common.docs.annotation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotasi OpenAPI untuk mendokumentasikan field version concurrency (ETag) dalam format Epoch Milliseconds.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
    ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Schema(
    description = "Versi timestamp/ETag terakhir (Epoch Milliseconds) untuk proteksi If-Match concurrency",
    example = "1731301234000",
    type = "integer",
    format = "int64")
public @interface VersionEpochSchema {
}
