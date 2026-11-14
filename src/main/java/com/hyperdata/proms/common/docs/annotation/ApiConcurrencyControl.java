package com.hyperdata.proms.common.docs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotasi OpenAPI gabungan untuk endpoint yang dilindungi concurrency control / optimistic locking.
 * Menggabungkan parameter If-Match header serta response 409 (STALE_VERSION) dan 428 (IF_MATCH_REQUIRED).
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@IfMatchHeader
@ApiResponseStaleVersion
@ApiResponseIfMatchRequired
public @interface ApiConcurrencyControl {
}
