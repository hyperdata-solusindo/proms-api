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
 * Anotasi OpenAPI standar untuk respon 404 Not Found.
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiResponse(
    responseCode = "404",
    description = "Resource tidak ditemukan",
    content = @Content(schema = @Schema(implementation = RestResponse.class)))
public @interface ApiResponseNotFound {
}
