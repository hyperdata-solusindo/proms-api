package com.hyperdata.proms.common.docs.annotation;

import com.hyperdata.proms.common.response.RestResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotasi OpenAPI standar untuk respon 409 Conflict akibat stale version / concurrency conflict.
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiResponse(
    responseCode = "409",
    description = "Konflik versi: data telah diperbarui oleh pengguna lain (STALE_VERSION)",
    content = @Content(schema = @Schema(implementation = RestResponse.class)))
public @interface ApiResponseStaleVersion {
}
