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
 * Anotasi OpenAPI standar untuk respon 428 Precondition Required saat header If-Match tidak disertakan.
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiResponse(
    responseCode = "428",
    description = "Precondition Required: header If-Match wajib disertakan (IF_MATCH_REQUIRED)",
    content = @Content(schema = @Schema(implementation = RestResponse.class)))
public @interface ApiResponseIfMatchRequired {
}
